import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;


/**
		 * Fixed Agent - Fixed bet
		 * @author Jose Fernandez 
		 * @version 17/01/2018
		 * https://github.com/jfdezgmez
		 */

public class psi3_Fixed extends Agent {
	private int id;
	private static int max = 0;

	protected void takeDown() {
		System.out.println("El fixedAgent ha terminado");
	}

	protected void setup() {

		System.out.println("Hola, soy el fixedAgent");
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		sd.setName("");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new MainBehaviour());
	}

	private class MainBehaviour extends CyclicBehaviour {
		/**
		 * Comportamiento ciclico encargado de la recepcion de los mensajes que
		 * llegan al fixed agent
		 */
		private static final long serialVersionUID = 1L;
		private ACLMessage message, myCoins, myBet;
		public int coinsOut;

		// Funcion encargada de la gestion de los distintos mensajes que le
		// llegan y la actuacion correcta respecto a ellos

		public void action() {
			message = receive();
			if (message != null) {
				if (message.getContent().startsWith("Id#") && message.getPerformative() == ACLMessage.INFORM) {
					getInfo();
				}
				if (message.getContent().startsWith("GetCoins") && message.getPerformative() == ACLMessage.REQUEST) {

					sendCoins(message.getContent().split("#")[1]);
				}

				if (message.getContent().startsWith("GuessCoins") && message.getPerformative() == ACLMessage.REQUEST) {
					sendGuessCoins();
				}
			} else {
				block();
			}
		}

		// Funcion encargada de la extraccion del ID asignado por el mainAgent

		private void getInfo() {
			String msg[] = message.getContent().split("#");
			id = Integer.parseInt(msg[1]);
		}

		// Funcion encargada del envio de la respuesta a los mensajes entrantes
		// con el formato GetCoins#
		// Selecciona un numero de fichas entre 0 y 3, ambos incluidos

		private void sendCoins(String x) {
			coinsOut = 3;
			String[] separada = x.split(",");
			max = (separada.length) * 3;
			myCoins = message.createReply();
			myCoins.setPerformative(ACLMessage.INFORM);
			myCoins.setContent("MyCoins#3");
			send(myCoins);
		}

		// Funcion encargada del envio de la respuesta a los mensajes entrantes
		// con el formato GuessCoins#
		// Selecciona un numero de fichas entre las que ha apostado y el numero
		// maximo, ambos incluidos

		private void sendGuessCoins() {
			int numero = (int) (Math.random() * (max + 1 - 3) + coinsOut);
			myBet = message.createReply();

			if (message.getContent().split("#").length != 1) {

				int aux[];
				String aux_string[] = (message.getContent().split("#")[1]).split(",");
				for (int i = 0; i < aux_string.length; i++) {

					if (numero == Integer.parseInt(aux_string[i])) {
						numero = (int) (Math.random() * (max + 1 - 3) + coinsOut);
						i = -1;
					}
				}
			}
			myBet.setPerformative(ACLMessage.INFORM);
			myBet.setContent("MyBet#" + numero);
			send(myBet);
		}

	}
}
