import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ThreadEscrita implements Runnable {

    private final int id;
    private final Socket socket;
    private final BlockingQueue<String> filaEcho;       // echo de mensagens do cliente
    private final BlockingQueue<String> filaBroadcast;  // mensagens vindas do servidor

    public ThreadEscrita(int id, Socket socket,
                         BlockingQueue<String> filaEcho,
                         BlockingQueue<String> filaBroadcast) {
        this.id             = id;
        this.socket         = socket;
        this.filaEcho       = filaEcho;
        this.filaBroadcast  = filaBroadcast;
    }

    @Override
    public void run() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("Conexão estabelecida com sucesso!");

            while (!socket.isClosed()) {

                // 1. Drena todos os broadcasts pendentes primeiro (sem bloquear)
                String broadcast;
                while ((broadcast = filaBroadcast.poll()) != null) {
                    out.println(broadcast);
                }

                // 2. Aguarda até 100 ms por um echo ou pelo POISON_PILL
                String echo = filaEcho.poll(100, TimeUnit.MILLISECONDS);

                if (echo == null) continue; // timeout — volta ao topo para checar broadcasts

                if (ThreadLeitura.POISON_PILL.equals(echo)) break; // encerramento solicitado

                out.println(echo);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[Escrita #" + id + "] Erro: " + e.getMessage());
        } finally {
            System.out.println("[Escrita #" + id + "] Thread encerrada.");
        }
    }
}