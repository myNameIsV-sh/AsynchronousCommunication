# TODO: implementar a cliente socket
import socket

def iniciar_cliente(host='127.0.0.1', port=65432):
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    
    try:
        client.connect((host, port))
        print("Conectado ao servidor! Digite 'sair' para encerrar.")
        
        while True:
            mensagem = input("Mensagem para o servidor: ")
            
            if mensagem.lower() == 'sair':
                print("Encerrando conexão: Solicitado pelo usuário.")
                break
            
            client.sendall(mensagem.encode('utf-8'))
            
            # O recv() aguarda a resposta do servidor
            resposta = client.recv(1024)
            
            # Se o recv retorna vazio, o servidor fechou a conexão
            if not resposta:
                print("Encerrando conexão por inatividade: O servidor encerrou a sessão.")
                break
                
            print(f"Servidor: {resposta.decode('utf-8')}")

    except ConnectionRefusedError:
        print("Encerrando conexão: Falha ao conectar (servidor offline).")
    except ConnectionResetError:
        print("Encerrando conexão: O servidor reiniciou ou descartou a conexão.")
    except BrokenPipeError:
        print("Encerrando conexão: Falha na transmissão (servidor indisponível).")
    except Exception as e:
        print(f"Encerrando conexão: Ocorreu um erro inesperado ({e}).")
    finally:
        client.close()
        print("Conexão fechada com sucesso.")

if __name__ == "__main__":
    iniciar_cliente()