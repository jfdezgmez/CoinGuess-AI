import java.io.Serializable;
import java.util.Vector;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
		 * Intel Agent code - Explained on README
		 * @author Jose Fernandez 
		 * @version 17/01/2018
		 * https://github.com/jfdezgmez
		 */


public class psi3_Intel0 extends Agent {
	private int id;
	private static int max = 0;
	public static int lastGuess;

	// Valores del codigo para la gestion del algoritmo y los calculos
	// posteriores

	final static double ALPHA = 0.6;
	private static final long serialVersionUID = 1L;
	double dAlpha = ALPHA;
	final static double dGamma = 0.4;
	final static double dEpsilon = 0.2;
	final static double dDecFactorLR = 0.99;
	final static double dMINLearnRate = 0.3;
	double dQmax;
	StateAction oLastStateAction;

	StateAction oPresentStateAction;
	Vector<StateAction> oVStateActions;

	protected void takeDown() {
		System.out.println("El intelAgent creado por Jose Fernandez ha terminado");
	}

	protected void setup() {
		System.out.println("Hola, soy el intelAgent creado por Jose Fernandez - PSI3");
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

		// Comportamiento ciclico encargado de la recepcion de los mensajes que
		// llegan al intel agent

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
					oVStateActions = new Vector<StateAction>();
				}
				if (message.getContent().startsWith("GetCoins") && message.getPerformative() == ACLMessage.REQUEST) {

					sendCoins(message.getContent().split("#")[1]);
				}

				if (message.getContent().startsWith("GuessCoins") && message.getPerformative() == ACLMessage.REQUEST) {

					sendGuessCoins();
				}
				if (message.getContent().startsWith("Result") && message.getPerformative() == ACLMessage.INFORM) {
					updateValueAction(message.getContent());
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

			String[] separada = x.split(",");
			max = separada.length * 3;
			myCoins = message.createReply();
			myCoins.setPerformative(ACLMessage.INFORM);

			int numero = (int) (Math.random() * 4);
			coinsOut = numero;
			myCoins.setContent("MyCoins#" + numero);
			send(myCoins);

		}

		// Funcion encargada del envio de la respuesta a los mensajes entrantes
		// con el formato GuessCoins#
		// Selecciona un numero de fichas entre las que ha apostado y el numero
		// maximo, ambos incluidos

		private void sendGuessCoins() {

			// Llamada a la funcion encargada de la gestion del algoritmo
			// Q-learning

			int numero = vGetNewActionQLearning(coinsOut, max, -1);

			myBet = message.createReply();

			if (message.getContent().split("#").length != 1) {

				int aux[];
				String aux_string[] = (message.getContent().split("#")[1]).split(",");
				for (int i = 0; i < aux_string.length; i++) {

					if (numero == Integer.parseInt(aux_string[i])) {

						numero = vGetNewActionQLearning(coinsOut, max, numero);
						i = -1;
					}
				}
			}

			lastGuess = numero;
			myBet.setPerformative(ACLMessage.INFORM);
			myBet.setContent("MyBet#" + numero);
			send(myBet);

		}

		// Funcion empleada por el algoritmo Q-learning para el descubrimiento
		// en caso de por ejemplo, no disponer de una buena opcion

		public int jugadaDescubrir() {
			int numero = (int) (Math.random() * (max + 1 - 3) + coinsOut);
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
			return numero;
		}

		// Funcion gestora del algoritmo usado por el agente inteligente

		public int vGetNewActionQLearning(int sState, int iNActions, int distinto) {
			boolean bFound;
			int iBest = -1, iNumBest = 1;

			StateAction oStateAction;
			int iNewAction;

			bFound = false;

			// Comienza la busqueda para consultar si el estado ya se ha creado
			// con anterioridad
			// El estado, como se comento en el fichero readme, viene dado
			// realmente por el par (numero de fichas escondidas, numero de
			// jugadores en esta ronda*3)

			for (int i = 0; i < oVStateActions.size(); i++) {

				oStateAction = (StateAction) oVStateActions.elementAt(i);

				if (oStateAction.sState == sState && iNActions + 1 == oStateAction.dValAction.length) {
					oPresentStateAction = oStateAction;
					bFound = true;
					break;
				}
			}

			// Si no lo encuentra, se dispone a crearlo

			if (!bFound) {

				oPresentStateAction = new StateAction(sState, iNActions);
				oVStateActions.add(oPresentStateAction);
			}
			;

			// Nos disponemos a bscar la accion mejor posicionada dentro del
			// vector de acciones

			dQmax = 0;

			for (int i = 0; i < iNActions + 1; i++) {

				// Buscamos el valor mayor, es decir, la mejor accion

				if (oPresentStateAction.dValAction[i] > dQmax) {
					iBest = i;
					iNumBest = 1;
					dQmax = oPresentStateAction.dValAction[i];

					// En caso de haber varios valores buenos nos decidimos por
					// uno aleatoriamente

				} else if ((oPresentStateAction.dValAction[i] == dQmax) && (dQmax > 0)) {
					iNumBest++;
					if (Math.random() < 1.0 / (double) iNumBest) {
						iBest = i;
						dQmax = oPresentStateAction.dValAction[i];
					}
				}
			}

			// Empleamos e-greedy

			if (((iBest > -1) && (Math.random() > dEpsilon))) {

				iNewAction = iBest;
			} else {

				// En caso de no poder usar e-greedy, realizamos una jugada
				// aleatoria que se decide por una apuesta dentro de los limites
				// acotados

				iNewAction = jugadaDescubrir();
			}
			oLastStateAction = oPresentStateAction;

			// Como esta indicado en el fichero readme, reducimos el valor de
			// aprendizaje

			dAlpha *= dDecFactorLR;
			if (dAlpha < dMINLearnRate) {
				dAlpha = dMINLearnRate;
			}
			return iNewAction;
		}

		// Funcion encargada de la actualizacion de la matriz en funcion a los
		// resultados obtenidos
		// La variacion de la misma va en funcion del resultado final correcto,
		// es decir, la apuesta correcta dada por el mensaje result

		public void updateValueAction(String result) {
			double reward;
			StateAction oStateAction;
			int resultadoReal, resultadoElegido;
			if (oLastStateAction != null) {
				String tmp[] = result.split("#");
				if (!tmp[1].equals("")) {

					if (Integer.parseInt(tmp[1]) == id) {
						reward = 0.02;
						oLastStateAction.dValAction[lastGuess] += dAlpha
								* (reward + dGamma * dQmax - oLastStateAction.dValAction[lastGuess]);
					} else {

						reward = -0.02;
						oLastStateAction.dValAction[lastGuess] += dAlpha
								* (reward + dGamma * dQmax - oLastStateAction.dValAction[lastGuess]);
						reward = 0.02;
						oLastStateAction.dValAction[Integer.parseInt(tmp[2])] += dAlpha
								* (reward + dGamma * dQmax - oLastStateAction.dValAction[Integer.parseInt(tmp[2])]);
					}

				} else {

					reward = -0.02;

					oLastStateAction.dValAction[lastGuess] += dAlpha
							* (reward + dGamma * dQmax - oLastStateAction.dValAction[lastGuess]);
					reward = 0.02;
					oLastStateAction.dValAction[Integer.parseInt(tmp[2])] += dAlpha
							* (reward + dGamma * dQmax - oLastStateAction.dValAction[Integer.parseInt(tmp[2])]);

				}

				for (int x = 0; x < oLastStateAction.dValAction.length; x++) {

				}

			}
		}

	}

	// Estados y acciones de q-learning

	class StateAction implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		int sState;
		double[] dValAction;

		StateAction(int sAuxState, int iNActions) {
			sState = sAuxState;
			dValAction = new double[iNActions + 1];
		}

		StateAction(int sAuxState, int iNActions, boolean bLA) {
			this(sAuxState, iNActions);
			if (bLA)
				for (int i = 0; i < iNActions; i++)
					dValAction[i] = 1.0 / iNActions;
		}

		public int sGetState() {
			return sState;
		}

		public double dGetQAction(int i) {
			return dValAction[i];
		}

		public void printQ() {
			System.out.println("La matriz Q: ");
			for (int i = 0; i < dValAction.length; i++) {
				System.out.println(" i " + i + " : " + dValAction[i]);
			}
		}

	}

}
