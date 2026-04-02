const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions");
const admin = require("firebase-admin");
const { GoogleGenerativeAI } = require("@google/generative-ai");

admin.initializeApp();

setGlobalOptions({ maxInstances: 10, region: "us-central1" });

// We initialize the model INSIDE the function now to access the secret
exports.categorizeNewTag = onDocumentCreated(
    {
        document: "tags/{tagId}",
        secrets: ["GEMINI_API_KEY"] // This gives the function access to your secret
    },
    async (event) => {
        const snapshot = event.data;
        if (!snapshot) return null;

        const tagData = snapshot.data();
        const tagName = tagData.name || event.params.tagId;

        if (tagData.status === "categorized") return null;

        // Initialize Gemini using the secret from process.env
        // UPDATED: Using the flagship Flash model gemini-3-flash-preview
        const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
        const model = genAI.getGenerativeModel({ model: "gemini-3-flash-preview" });

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

            const prompt = `
                Pick 1-3 categories for the tag "${tagName}" from this list: ${categories.join(", ")}.
                Return ONLY a comma-separated list. If none fit, return "Other".
            `;

            const result = await model.generateContent(prompt);
            const response = await result.response;
            const aiText = response.text().trim();

            const assignedParents = aiText.split(",")
                .map(item => item.trim())
                .filter(item => categories.includes(item) || item === "Other");

            return snapshot.ref.update({
                parents: assignedParents,
                status: "categorized",
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            });

        } catch (error) {
            console.error("Categorization failed:", error);
            return null;
        }
    }
);
