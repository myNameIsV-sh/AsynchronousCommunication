import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ThreadEscrita implements Runnable {
    private final int id;
    private final Socket socket;
    private final BlockingQueue<String> filaSaida;

    public ThreadEscrita(int id, Socket socket, BlockingQueue<String> filaSaida) {
        this.id = id;
        this.socket = socket;
        this.filaSaida = filaSaida;
    }

    @Override
    public void run() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("Conexão estabelecida com sucesso!");

            while (!socket.isClosed()) {
                // take() bloqueia até que algo entre na fila
                String msg = filaSaida.take();
                out.println(msg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[Escrita #" + id + "] Erro: " + e.getMessage());
        }
    }
}