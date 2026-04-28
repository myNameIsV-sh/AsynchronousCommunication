import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Servidor {

    private final int porta;
    private ServerSocket serverSocket;
    private final AtomicInteger contadorId = new AtomicInteger(0);
    private static final int MAX_CONEXOES = 5;
    private final Semaphore semaforo = new Semaphore(MAX_CONEXOES);

    private final ConcurrentHashMap<Integer, SessaoCliente> sessoesAtivas = new ConcurrentHashMap<>();

    public Servidor(int porta) {
        this.porta = porta;
    }

    public void iniciar() {
        // Inicia a thread administrativa para monitorar o console do servidor
        monitorarComandos();

        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Servidor iniciado na porta " + porta);
            System.out.println("Aguardando conexões (Limite: " + MAX_CONEXOES + ")...");

            while (!serverSocket.isClosed()) {
                try {
                    Socket cliente = serverSocket.accept();
                    String enderecoCliente = cliente.getInetAddress().getHostAddress();

                    // Tenta adquirir uma vaga no semáforo sem bloquear a thread principal
                    if (semaforo.tryAcquire()) {
                        int idCliente = contadorId.incrementAndGet();
                        atenderCliente(idCliente, cliente);
                    } else {
                        System.out.println("Limite de conexões atingido para: " + enderecoCliente);
                        fecharSocketImediatamente(cliente);
                    }
                } catch (SocketException e) {
                    if (serverSocket.isClosed()) {
                        System.out.println("Servidor parou de aceitar novas conexões.");
                    } else {
                        System.err.println("Erro no accept: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro fatal no servidor: " + e.getMessage());
        }
    }

    private void atenderCliente(int idCliente, Socket cliente) {
        BlockingQueue<String> filaEcho      = new LinkedBlockingQueue<>();
        BlockingQueue<String> filaBroadcast = new LinkedBlockingQueue<>();

        SessaoCliente sessao = new SessaoCliente(cliente, filaEcho, filaBroadcast);
        sessoesAtivas.put(idCliente, sessao); // registro antes de iniciar as threads

        Runnable callbackFechamento = () -> encerrarConexao(idCliente, cliente);

        Thread tLeitura = new Thread(new ThreadLeitura(idCliente, cliente, filaEcho, callbackFechamento));
        Thread tEscrita = new Thread(new ThreadEscrita(idCliente, cliente, filaEcho, filaBroadcast));

        tLeitura.start();
        tEscrita.start();
    }

    private void monitorarComandos() {
        Thread adminThread = new Thread(() -> {
            Scanner console = new Scanner(System.in);
            System.out.println("[ADMIN] Digite 'sair' a qualquer momento para encerrar o servidor.");
            while (true) {
                if (console.hasNextLine()) {
                    String comando = console.nextLine();
                    if (comando.equalsIgnoreCase("sair") || comando.equalsIgnoreCase("encerrar")) {
                        fechar();
                        break;
                    }
                }
            }
        });
        adminThread.setDaemon(true); // Garante que a thread feche se o servidor parar
        adminThread.start();
    }

    // Método sincronizado para garantir que o encerramento ocorra apenas uma vez por cliente
    private synchronized void encerrarConexao(int idCliente, Socket cliente) {
        try {
            if (cliente != null && !cliente.isClosed()) {
                cliente.close();
                semaforo.release();
                sessoesAtivas.remove(idCliente); // limpa o registro
                System.out.println("[Servidor] Cliente #" + idCliente + " desconectado. Vaga liberada.");
            }
        } catch (IOException e) {
            System.err.println("Erro ao encerrar conexão do cliente #" + idCliente + ": " + e.getMessage());
        }
    }

    private void fecharSocketImediatamente(Socket cliente) {
        try {
            cliente.close();
        } catch (IOException e) {
            // Ignora erro ao fechar conexão recusada
        }
    }

    public void fechar() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                System.out.println("[Servidor] Fechando socket principal...");
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar o servidor: " + e.getMessage());
        }
    }
}