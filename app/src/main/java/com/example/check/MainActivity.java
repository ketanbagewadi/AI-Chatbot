package com.example.check;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.check.R;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.FixedHeaderProvider;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.ai.generativelanguage.v1beta2.DiscussServiceClient;
import com.google.ai.generativelanguage.v1beta2.DiscussServiceSettings;
import com.google.ai.generativelanguage.v1beta2.GenerateMessageRequest;
import com.google.ai.generativelanguage.v1beta2.GenerateMessageResponse;
import com.google.ai.generativelanguage.v1beta2.Message;
import com.google.ai.generativelanguage.v1beta2.MessagePrompt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PALM_API_KEY = "YOUR_PALM_API_KEY";

    private DiscussServiceClient client;
    private List<Message> conversation;
    private LinearLayout chatContainer;
    private EditText userInputField;
    private Button sendButton;
    private ImageButton voiceInputButton;

    // SpeechRecognizer and its intent
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the DiscussServiceClient
        HashMap<String, String> headers = new HashMap<>();
        headers.put("x-goog-api-key", PALM_API_KEY);

        InstantiatingGrpcChannelProvider provider = InstantiatingGrpcChannelProvider.newBuilder()
                .setHeaderProvider(FixedHeaderProvider.create(headers))
                .build();

        DiscussServiceSettings settings = null;
        try {
            settings = DiscussServiceSettings.newBuilder()
                    .setTransportChannelProvider(provider)
                    .setCredentialsProvider(FixedCredentialsProvider.create(null))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            client = DiscussServiceClient.create(settings);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize the conversation list
        conversation = new ArrayList<>();

        // Set up UI components
        chatContainer = findViewById(R.id.chatContainer);
        userInputField = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        voiceInputButton = findViewById(R.id.voiceInputButton); // Add this line

        // Set a click listener for the send button
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the user input
                String userInput = userInputField.getText().toString().trim();

                if (!userInput.isEmpty()) {
                    // Create a message from user input
                    Message userMessage = Message.newBuilder()
                            .setAuthor("user")
                            .setContent(userInput)
                            .build();

                    // Add the user message to the conversation
                    conversation.add(userMessage);

                    // Generate a response from the chatbot
                    generateMessage();

                    // Clear the user input field
                    userInputField.setText("");

                    // Execute generateMessage on a background thread
                    new GenerateMessageTask().execute();
                }
            }
        });

        // Set up the voice input button click listener
        voiceInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start listening for voice input
                speechRecognizer.startListening(speechIntent);
            }
        });

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new MyRecognitionListener());

        // Initialize SpeechRecognizer Intent
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    }

    // ... Your existing code ...
    // AsyncTask to execute generateMessage on a background thread
    private class GenerateMessageTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            generateMessage();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Display the conversation in the UI after generating the message
            displayConversation();
        }

        public void execute() {

        }
    }

    private void generateMessage() {
        // Create the GenerateMessageRequest
        GenerateMessageRequest request = GenerateMessageRequest.newBuilder()
                .setModel("models/chat-bison-001")
                .setPrompt(createMessagePrompt())
                .setTemperature(0.5f)
                .setCandidateCount(1)
                .build();

        // Send the request and get the response
        GenerateMessageResponse response;
        try {
            response = client.generateMessage(request);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
            return;
        }

        // Get the returned message
        Message returnedMessage = response.getCandidatesList().get(0);

        // Add the chatbot's response to the conversation
        conversation.add(returnedMessage);

        // Display the conversation in the UI
        displayConversation();
    }


    // RecognitionListener to handle speech recognition events
    private class MyRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            // Called when the SpeechRecognizer is ready to receive user speech
        }

        @Override
        public void onBeginningOfSpeech() {
            // Called when the user starts speaking
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            // Called when the RMS dB (volume) of the user's speech changes
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            // Called when partial recognition results are available
        }

        @Override
        public void onEndOfSpeech() {
            // Called when the user stops speaking
        }

        @Override
        public void onError(int error) {
            // Called when an error occurs during speech recognition
        }

        @Override
        public void onResults(Bundle results) {
            // Called when the recognition is successful
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                // Get the first match as the user input
                String userInput = matches.get(0);

                // Process the user input (e.g., display in the input field and proceed with generating a response)
                userInputField.setText(userInput);

                // Rest of your code to generate a response and display the conversation
                // Send the request and get the response
                GenerateMessageResponse response;
                try {
                    GenerateMessageRequest request = null;
                    response = client.generateMessage(request);
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    return;
                }

                // Get the returned message
                Message returnedMessage = response.getCandidatesList().get(0);

                // Add the chatbot's response to the conversation
                conversation.add(returnedMessage);

                // Display the conversation in the UI
                displayConversation();
            }

            // Execute generateMessage on a background thread
            new GenerateMessageTask().execute();
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }



    // ... Your existing code ...
    private MessagePrompt createMessagePrompt() {
        // Create the message prompt and examples
        MessagePrompt.Builder messagePromptBuilder = MessagePrompt.newBuilder();

        // Add all messages in the conversation
        for (Message message : conversation) {
            messagePromptBuilder.addMessages(message);
        }

        return messagePromptBuilder.build();
    }
    private void displayConversation() {
        chatContainer.removeAllViews();

        for (Message message : conversation) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            if (message.getAuthor().equals("user")) {
                // Sent message bubble
                TextView sentMessageTextView = new TextView(this);
                sentMessageTextView.setText(message.getContent());
                sentMessageTextView.setTextColor(getResources().getColor(android.R.color.black));
                sentMessageTextView.setBackgroundResource(R.drawable.sent_message_bubble);
                sentMessageTextView.setPadding(16, 8, 16, 8);
                layoutParams.setMargins(200, 8, 8, 8);
                sentMessageTextView.setLayoutParams(layoutParams);
                chatContainer.addView(sentMessageTextView);
            } else {
                // Received message bubble
                TextView receivedMessageTextView = new TextView(this);
                receivedMessageTextView.setText(message.getContent());
                receivedMessageTextView.setTextColor(getResources().getColor(android.R.color.black));
                receivedMessageTextView.setBackgroundResource(R.drawable.received_message_bubble);
                receivedMessageTextView.setPadding(16, 8, 16, 8);
                layoutParams.setMargins(8, 8, 200, 8);
                receivedMessageTextView.setLayoutParams(layoutParams);
                chatContainer.addView(receivedMessageTextView);
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Release SpeechRecognizer resources
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        // Close the DiscussServiceClient
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.onDestroy();
    }
}
