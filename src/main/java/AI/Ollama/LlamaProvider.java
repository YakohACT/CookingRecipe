package main.java.AI.Ollama;

/**
 * Meta Llama 系モデル(Ollama 経由)
 */
public class LlamaProvider extends OllamaProvider {

    @Override
    public String[] getAvailableModels() {
        return new String[]{
                "llama3:8b"
        };
    }
}
