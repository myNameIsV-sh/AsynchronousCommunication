import socket

class Client:
    def __init__(self, host, porta):
        self.host = host
        self.porta = porta
        self.socket = None
        self.reader = None

    def conectar(self):
        """Estabelece a conexão com o servidor."""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.host, self.porta))
            self.reader = self.socket.makefile('r', encoding='utf-8')
            print("O cliente se conectou ao servidor!")
        except ConnectionRefusedError:
            print("Erro ao conectar: Conexão recusada (servidor offline).")
        except Exception as e:
            print(f"Erro inesperado ao conectar: {e}")

    def enviar_mensagem(self, mensagem):
        """Envia uma mensagem para o servidor."""
        if not self.esta_conectado():
            print("Cliente não está conectado. Chame conectar() primeiro.")
            return False

        try:
            self.socket.sendall((mensagem + "\n").encode('utf-8'))
            return True
        except (ConnectionResetError, BrokenPipeError):
            print("Erro ao enviar: O servidor encerrou a conexão.")
            return False

    def receber_resposta(self):
        """
        Aguarda e recebe a resposta completa do servidor.

        O servidor Java usa println(), que envia cada linha individualmente
        sem um delimitador explícito de fim de mensagem. Para capturar
        respostas com múltiplas linhas (ex: HELP), usamos um timeout curto:
        lemos a primeira linha bloqueando normalmente, depois tentamos
        ler linhas adicionais dentro de 200 ms. Quando o timeout expira,
        consideramos a resposta completa.
        """
        if not self.esta_conectado():
            return None

        linhas = []

        try:
            # 1. Lê a primeira linha (bloqueante — aguarda o servidor responder)
            self.socket.settimeout(None)
            primeira = self.reader.readline()
            if not primeira:
                print("O servidor encerrou a sessão.")
                return None
            linhas.append(primeira.rstrip('\r\n'))

            # 2. Tenta ler linhas adicionais com timeout curto
            self.socket.settimeout(0.2)
            while True:
                try:
                    linha = self.reader.readline()
                    if not linha:
                        break
                    linhas.append(linha.rstrip('\r\n'))
                except socket.timeout:
                    break  # sem mais dados no intervalo — resposta completa

        except socket.timeout:
            pass  # improvável na leitura bloqueante, mas tratado por segurança
        except Exception as e:
            print(f"Erro ao receber dados: {e}")
            return None
        finally:
            # Restaura modo bloqueante para a próxima chamada
            if self.socket:
                self.socket.settimeout(None)

        return "\n".join(linhas)

    def receber_linha(self):
        """
        Lê exatamente uma linha do servidor (para o handshake inicial de ID
        e boas-vindas, onde cada mensagem é de uma linha só).
        """
        if not self.esta_conectado():
            return None

        try:
            linha = self.reader.readline()
            if not linha:
                print("O servidor encerrou a sessão.")
                return None
            return linha.strip()
        except Exception as e:
            print(f"Erro ao receber dados: {e}")
            return None

    def fechar(self):
        """Fecha a conexão com o servidor."""
        if self.socket:
            self.socket.close()
            self.socket = None
            self.reader = None
            print("Conexão encerrada.")

    def esta_conectado(self):
        """Verifica se o socket ainda é válido."""
        return self.socket is not None