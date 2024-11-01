const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendHelperNotification = functions.firestore
  .document("requests/{requestId}")
  .onCreate((snap, context) => {
    const requestData = snap.data();

    // Fetch helper's FCM token (stored in Firestore or provided during login)
    return admin.firestore().collection("helpers").get().then((snapshot) => {
      let tokens = [];
      snapshot.forEach((doc) => {
        tokens.push(doc.data().fcmToken); // Assuming each helper doc has a field "fcmToken"
      });

      const payload = {
        notification: {
          title: "New Service Request",
          body: `${requestData.username} requested ${requestData.service} service.`,
        },
        data: {
          requestId: context.params.requestId,  // Pass request ID for further processing
          service: requestData.service,
          username: requestData.username
        }
      };

      return admin.messaging().sendToDevice(tokens, payload)
        .then((response) => {
          console.log("Notification sent successfully:", response);
        })
        .catch((error) => {
          console.error("Error sending notification:", error);
        });
    });
  });
