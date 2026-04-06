const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions");
const admin = require("firebase-admin");
const { GoogleGenerativeAI } = require("@google/generative-ai");

admin.initializeApp();

setGlobalOptions({ maxInstances: 10, region: "us-central1" });

/**
 * Cloud Function to automatically categorize event tags using Google Gemini AI.
 * Updated to use onDocumentWritten to handle re-categorization and recovery of stuck tags.
 */
exports.categorizeTag = onDocumentWritten(
    {
        document: "tags/{tagId}",
        secrets: ["GEMINI_API_KEY"]
    },
    async (event) => {
        const snapshot = event.data.after;
        if (!snapshot || !snapshot.exists()) return null;

        const tagData = snapshot.data();
        const tagId = event.params.tagId;
        const tagName = tagData.name || tagId;

        // Skip if already categorized or being deleted
        if (tagData.status === "categorized") return null;

        try {
            const metaDoc = await admin.firestore().collection("metadata").doc("categories").get();

            let categories = [
                "Sports", "Aquatics", "Music", "Performance", "Arts", "Wellness",
                "Education", "Tech", "Outdoors", "Social", "Career", "Family",
                "Culinary", "Gaming", "Travel", "Finance", "Community", "Health",
                "Pets", "Science", "Hobbies"
            ];

            if (metaDoc.exists && metaDoc.data().list) {
                categories = metaDoc.data().list;
            }

            // OPTIMIZATION: If the tag is exactly a category name, categorize it immediately
            const directMatch = categories.find(c => c.toLowerCase() === tagName.toLowerCase());
            if (directMatch) {
                return snapshot.ref.update({
                    parents: [directMatch],
                    status: "categorized",
                    lastUpdated: admin.firestore.FieldValue.serverTimestamp()
                });
            }

            // Initialize Gemini
            const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
            const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

            const prompt = `
                Analyze the event tag "${tagName}".
                Pick 1-3 most relevant parent categories from this list: ${categories.join(", ")}.
                Return ONLY a comma-separated list of the category names.
                If none fit well, return "Other".
            `;

            const result = await model.generateContent(prompt);
            const response = await result.response;
            const aiText = response.text().trim();

            // Filter AI results against allowed categories (case-insensitive)
            const assignedParents = aiText.split(",")
                .map(item => item.trim())
                .filter(item => categories.some(cat => cat.toLowerCase() === item.toLowerCase()) || item.toLowerCase() === "other")
                .map(item => {
                    const found = categories.find(cat => cat.toLowerCase() === item.toLowerCase());
                    return found || "Other";
                });

            return snapshot.ref.update({
                parents: assignedParents,
                status: "categorized",
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            });

        } catch (error) {
            console.error("Categorization failed for tag:", tagName, error);
            return null;
        }
    }
);
