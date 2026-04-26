import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

// TODO: Limitar o servidor para atender no MÁXIMO cinco conexões
//   * Soluções possíveis:
//   * Contador de Conexões Ativas: Mantenha uma variável global (ou atômica) que incrementa no accept() e decrementa no close(). Se o
//     limite for atingido, você pode fechar a nova conexão imediatamente ou enviar uma mensagem de "servidor cheio".
//   * Parâmetro backlog no listen(): Ao chamar listen(fd, backlog), o backlog define o tamanho da fila de conexões pendentes (que ainda
//     não foram aceitas pelo accept()). Conexões além disso serão recusadas pelo SO com um erro de "Connection Refused".
//   * Pool de Workers: Se você usa um modelo de threads ou processos, limitar o tamanho do pool limita inerentemente quantas conexões
//     podem ser processadas simultaneamente.
public class Servidor {

    private final int porta;
    private ServerSocket serverSocket;

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

//                System.out.println("Nova conexão com o cliente " +
//                        cliente.getInetAddress().getHostAddress());

                if (semaforo.tryAcquire()) {
                    System.out.println("Nova conexão com o cliente " + enderecoCliente +
                            " | Conexões ativas: " + (MAX_CONEXOES - semaforo.availablePermits()));
                    // À medida que novas conexões são estabelecidas, esses clientes ganham novas "threads".
                    Thread thread = new Thread(() -> atenderCliente(cliente));
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

    private void atenderCliente(Socket cliente) {
        try {
            Scanner s = new Scanner(cliente.getInputStream());

            // Pontos bloqueantes
            while (s.hasNextLine()) {
                // Pontos bloqueantes
                System.out.println(s.nextLine());
            }

            s.close();
            cliente.close();

        } catch (IOException e) {
            System.err.println("Erro ao atender cliente: " + e.getMessage());
        } finally {
            semaforo.release();
            // Feedback de desconexão do cliente
            System.out.println("Cliente " + cliente.getInetAddress().getHostAddress() +
                    " desconectado. | Conexões ativas: " + (MAX_CONEXOES - semaforo.availablePermits()));
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