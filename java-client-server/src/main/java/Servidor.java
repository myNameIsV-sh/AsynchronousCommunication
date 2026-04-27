import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Servidor {

    private final int porta;
    private ServerSocket serverSocket;

    private final AtomicInteger contadorId = new AtomicInteger(0);

    private static final int MAX_CONEXOES = 5;
    private final Semaphore semaforo = new Semaphore(MAX_CONEXOES);

    public Servidor(int porta) {
        this.porta = porta;
    }

    public void iniciar() {
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Porta " + porta + " aberta!");

            while (true) {
                // Pontos bloqueantes
                Socket cliente = serverSocket.accept();
                String enderecoCliente = cliente.getInetAddress().getHostAddress();

                if (semaforo.tryAcquire()) {
                    int idCliente = contadorId.incrementAndGet();
                    System.out.println("Nova conexão com o cliente " + enderecoCliente +
                            " | Conexões ativas: " + (MAX_CONEXOES - semaforo.availablePermits()));

                    Thread thread = new Thread(() -> atenderCliente(idCliente, cliente));
                    thread.start();
                } else {
                    System.out.println("Conexão recusada (limite atingido): " + enderecoCliente);
                    cliente.close();
                }
            }

        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    private void atenderCliente(int idCliente, Socket cliente) {
        try {
            Scanner s = new Scanner(cliente.getInputStream());

            // Pontos bloqueantes
            while (s.hasNextLine()) {
                // Pontos bloqueantes
                System.out.println("[Cliente #" + idCliente + "] " + s.nextLine());
            }

            s.close();
            cliente.close();

        } catch (IOException e) {
            System.err.println("[Cliente #" + idCliente + "] Erro: " + e.getMessage());
        } finally {
            semaforo.release();
            // Feedback de desconexão do cliente
            System.out.println("[Cliente #" + idCliente + "] Desconectado | Conexões ativas: " +
                    (MAX_CONEXOES - semaforo.availablePermits()));
        }
    }

    public void fechar() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar o servidor: " + e.getMessage());
        }
    }
}