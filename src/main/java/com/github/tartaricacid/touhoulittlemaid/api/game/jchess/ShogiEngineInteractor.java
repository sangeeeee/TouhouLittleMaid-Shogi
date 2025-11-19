package com.github.tartaricacid.touhoulittlemaid.api.game.jchess;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShogiEngineInteractor {
    private Process process;
    private BufferedReader reader;
    private PrintWriter writer;
    private final String enginePath = EngineExtractor.getEnginePath().toString();
    private final String evalDir    = EngineExtractor.getEvalDir().toString();
    private final String bookDir    = EngineExtractor.getBookPath().getParent().toString();

    // Setup method: Initializes the engine and sends setup commands
    public String setup(String jsonParams) throws IOException, InterruptedException {
        // Start the engine process
        ProcessBuilder pb = new ProcessBuilder(enginePath);
        process = pb.start();

        // Get input/output streams
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);

        // Send USI command
        writer.println("usi");
        // Read responses until "usiok"
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("usiok")) {
                break;
            }
        }

        // Parse JSON params if provided
        if (jsonParams != null && !jsonParams.isEmpty()) {
            try {
                Gson gson = new Gson();
                JsonObject options = gson.fromJson(jsonParams, JsonObject.class);
                for (String key : options.keySet()) {
                    String value = options.get(key).getAsString();
                    writer.println("setoption name " + key + " value " + value);
                }
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage());
            }
        }
        writer.println("setoption name EvalDir value " + evalDir);
        writer.println("setoption name bookDir value " + bookDir);

        // Send isready
        writer.println("isready");
        // Wait for readyok
        while ((line = reader.readLine()) != null) {
            if (line.equals("readyok")) {
                break;
            }
        }

        return "setup ok";
    }

    // Interaction method: Sends position and go, returns bestmove or error
    public String interact(String sfen, String moves) throws IOException, InterruptedException {
        if (process == null || !process.isAlive()) {
            throw new IllegalStateException("Engine not setup or stopped.");
        }

        // Build position command
        StringBuilder positionCmd = new StringBuilder("position sfen ");
        positionCmd.append(sfen);
        if (moves != null && !moves.isEmpty()) {
            // Restrict to one move as per requirement
            positionCmd.append(" moves ").append(moves);
        }

        // Send position
        writer.println(positionCmd.toString());

        // Check for immediate error with timeout
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> reader.readLine());
        String errorLine = null;
        try {
            errorLine = future.get(100, TimeUnit.MILLISECONDS); // Short timeout to check if output is available
        } catch (TimeoutException e) {
            // No immediate output, assume no error
        } catch (ExecutionException | InterruptedException e) {
            throw new IOException("Error reading from engine", e);
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
            }
            executor.shutdownNow();
        }

        Pattern errorPattern = Pattern.compile("info string Error! : (.*)");
        if (errorLine != null) {
            Matcher matcher = errorPattern.matcher(errorLine);
            if (matcher.matches()) {
                // Error detected, return immediately
                writer.println("stop"); // Send stop to engine if needed
                return "Error: " + matcher.group(1);
            } else {
                // Unexpected output, but continue or handle as needed
                // For now, proceed, but could log or something
            }
        }

        // If no error, send go
        writer.println("go movetime 3000");

        // Wait for bestmove
        Pattern bestmovePattern = Pattern.compile("bestmove (\\S+).*");
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[Engine] "+ line);
            Matcher matcher = bestmovePattern.matcher(line);
            if (matcher.matches()) {
                String bestMove = matcher.group(1);
                if ("resign".equals(bestMove) || "win".equals(bestMove)) {
                    return bestMove;
                }
                return bestMove;
            }
        }

        throw new IOException("No bestmove received.");
    }

    // Stop method: Sends quit and destroys the process
    public String stop() {
        if (process != null && process.isAlive()) {
            writer.println("quit");
            // No need to wait for response as per requirement
            process.destroy();
            // Wait briefly to ensure destruction
            try {
                process.waitFor(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return "stop ok";
    }

}