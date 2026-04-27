import java.util.Scanner;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ThreadLeitura implements Runnable {
    private final int id;
    private final Socket socket;
    private final BlockingQueue<String> filaSaida;
    private final Runnable aoEncerrar;

    public ThreadLeitura(int id, Socket socket, BlockingQueue<String> filaSaida, Runnable aoEncerrar) {
        this.id = id;
        this.socket = socket;
        this.filaSaida = filaSaida;
        this.aoEncerrar = aoEncerrar;
    }

    @Override
    public void run() {
        try (Scanner in = new Scanner(socket.getInputStream())) {
            // cliente.setSoTimeout(30000); // Comentado conforme solicitado

            while (in.hasNextLine()) {
                String msg = in.nextLine();
                System.out.println("[Leitura #" + id + "] " + msg);

                if (msg.equalsIgnoreCase("sair")) break;

                // Coloca na fila para a outra thread enviar
                filaSaida.put("Servidor recebeu sua mensagem: " + msg);
            }
        } catch (Exception e) {
            System.err.println("[Leitura #" + id + "] Erro: " + e.getMessage());
        } finally {
            aoEncerrar.run(); // Executa o callback de fechamento
        }
    }
}