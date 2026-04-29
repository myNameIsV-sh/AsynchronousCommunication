import java.util.Random;

public class ProcessadorMensagem {

    private final int idCliente;
    private final Servidor servidor;
    private final Random random = new Random();

    public ProcessadorMensagem(int idCliente, Servidor servidor) {
        this.idCliente = idCliente;
        this.servidor  = servidor;
    }

    public String processar(String mensagem) {
        String[] partes = mensagem.trim().split(" ", 2);
        String tipo = partes[0].toUpperCase();
        String dados = partes.length > 1 ? partes[1] : "";

        switch (tipo) {
            case "HELP":
                return "Comandos disponíveis:\n" +
                        "  PING           — verifica se o servidor está ativo\n" +
                        "  UPTIME         — tempo de execução do servidor\n" +
                        "  RAND           — número aleatório entre 1 e 1000\n" +
                        "  PALINDROMO     — verifica se a frase é um pangrama. Ex: PALINDROMO the quick brown fox\n" +
                        "  VERSION        — informações sobre o servidor\n" +
                        "  HELP           — exibe esta mensagem";

            case "PING":
                return "PONG";

            case "UPTIME":
                return formatarUptime(servidor.getUptimeEmSegundos());

            case "RAND":
                return "Número aleatório: " + (random.nextInt(1000) + 1);

            case "PALINDROMO":
                if (dados.isEmpty()) return "ERRO: forneça uma frase. Ex: PALINDROMO the quick brown fox";
                return verificarPangrama(dados)
                        ? "\"" + dados + "\" é um pangrama!"
                        : "\"" + dados + "\" não é um pangrama.";

            case "VERSION":
                return "Feito com <3 por Victor Gustavo e Alice Tavares";

            default:
                return "ERRO: mensagem desconhecida — '" + tipo + "'";
        }
    }

    private String formatarUptime(long totalSegundos) {
        long horas   = totalSegundos / 3600;
        long minutos = (totalSegundos % 3600) / 60;
        long segundos = totalSegundos % 60;
        return String.format("Uptime: %02dh %02dm %02ds", horas, minutos, segundos);
    }

    private boolean verificarPangrama(String frase) {
        String lower = frase.toLowerCase();
        for (char c = 'a'; c <= 'z'; c++) {
            if (lower.indexOf(c) == -1) return false;
        }
        return true;
    }
}