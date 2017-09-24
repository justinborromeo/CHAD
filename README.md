## Inspiration

We were inspired to create CHAD by recent news about the passing of a law regarding distracted driving in Michigan.  Being from Canada (where such a law already exists), we know that a law is unable to completely eliminate distracted driving.  We wanted a way to minimize distracted driving while still letting users know if something important happened.

## What it does

CHAD is an Android app that uses machine learning to classify the importance of incoming texts.  If the text is deemed important, the user is audibly notified and given the option to verbally respond to the message.  Should the user wish to respond, the app will parse his/her voice and send a text message with the contents.  Furthermore, the app will filter out profanity.

## How we built it

We developed the mobile app using Kotlin in Android Studio, we built the classifier using SciKit-Learn and Keras, and we implemented the server using Python and Flask.  We used Google Cloud Platform as a machine learning endpoint and to host the backend.

## Challenges we ran into

As the majority of our team's members had never used Kotlin before, we had to learn on the fly.
