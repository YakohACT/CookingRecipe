package main.java.AI.Ollama;

/**
 * Google Gemma 系モデル(Ollama 経由)
 */
public class GemmaProvider extends OllamaProvider {

    @Override
    public String[] getAvailableModels() {
        return new String[]{
                "gemma4:e2b",
                "gemma4:e4b"
        };
    }
}
