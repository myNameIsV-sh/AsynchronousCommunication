import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class SessaoCliente {
    final Socket socket;
    final BlockingQueue<String> filaEcho;
    final BlockingQueue<String> filaBroadcast;

    SessaoCliente(Socket socket, BlockingQueue<String> filaEcho, BlockingQueue<String> filaBroadcast) {
        this.socket       = socket;
        this.filaEcho     = filaEcho;
        this.filaBroadcast = filaBroadcast;
    }
}