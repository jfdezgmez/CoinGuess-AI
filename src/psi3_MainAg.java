import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
		 * Main Agent 
		 * @author Jose Fernandez 
		 * @version 17/01/2018
		 * https://github.com/jfdezgmez
		 */


public class psi3_MainAg extends Agent {

	public psi3_GUI_panel Interfaz;
	private LinkedHashMap<AID, psi3_player> mapPlayers;
	private MainBehaviour mB = null;
	private int totalGames = 0;
	private int maxGames = 5;
	private boolean verbose = false;
	public boolean stop = false;

	protected void setup() {

		// Creacion de la interfaz grafica

		Interfaz = new psi3_GUI_panel(this);

	}

	// Funcion busqueda inicial de jugadores

	public void findPlayers() {

		// Comportamiento lanzado una sola vez para su busqueda

		addBehaviour(new OneShotBehaviour() {
			private static final long serialVersionUID = 1L;

			public void action() {

				// Actualiza el mapa de jugadores

				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("Player");
				template.addServices(sd);
				try {
					mapPlayers = new LinkedHashMap<AID, psi3_player>();
					AID[] sellerAgents;
					DFAgentDescription[] result = DFService.search(myAgent, template);
					sellerAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						sellerAgents[i] = result[i].getName();

						mapPlayers.put(result[i].getName(),
								new psi3_player(i, result[i].getName().getName().toString().split("@")[0], "Player",
										result[i].getName(), 0, 0, 0, false));
					}

					// Mostramos en la interfaz los datos recogidos

					Interfaz.cargaTabla();
					Interfaz.actualizaPantalla();
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}
			}
		});
		return;
	}

	public ArrayList<psi3_player> findPlayersArray() {
		return new ArrayList<psi3_player>(mapPlayers.values());
	}

	// Funciones para el cumplimiento de las funcionalidades de los distintos
	// botones implementados en la interfaz grafica

	public void Stop() {
		return;
	}

	public void Continue() {
		mB.restart();
		return;
	}

	public void New() {

		if (mB != null) {
			removeBehaviour(mB);

		}
		mB = new MainBehaviour();
		addBehaviour(mB);
		stop = false;
		for (AID keyset : mapPlayers.keySet()) {
			mapPlayers.get(keyset).setWins(0);
			mapPlayers.get(keyset).setLoses(0);
			mapPlayers.get(keyset).setGames(0);
			mapPlayers.get(keyset).setWon(false);
			totalGames = 0;
		}
	}

	// Funcion de aviso finalizacion del Agente principal

	protected void takeDown() {

		System.out.println("El mainAgent ha terminado");

	}

	// Comportamiento principal del main Agent

	class MainBehaviour extends Behaviour {
		private static final long serialVersionUID = 1L;
		int step = 0;
		public LinkedHashMap<String, Integer> mapCoins = new LinkedHashMap<String, Integer>();
		public LinkedHashMap<String, Integer> mapBets = new LinkedHashMap<String, Integer>();
		int numPlayers;
		int row, column;
		boolean huboGanador;
		String Winner;

		int totMessages;
		public int howManyInRound = 0;

		public void action() {

			// Switch que organiza las distintas fases para la sincronizacion de
			// los agentes participantes

			switch (step) {
			case 0:
				playersIdInf();
				break;
			case 1:
				if (!stop) {
					reqCoins();
				} else {
					block();
				}
				break;
			case 2:
				recCoins();
				break;
			case 3:
				reqGuessCoins();
				break;
			case 4:
				whoWins();

				break;
			case 5:
				try {
					TimeUnit.MILLISECONDS.sleep(30);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				actualizaResul();
				break;
			default:
				break;
			}

		}

		// FASE 0
		// Funcion encargada de la asignacion e informacion a los distintos
		// agentes de su ID

		private void playersIdInf() {
			for (AID keyset : mapPlayers.keySet()) {

				ACLMessage playerInf = new ACLMessage(ACLMessage.INFORM);
				playerInf.addReceiver(keyset);

				playerInf.setContent("Id#" + mapPlayers.get(keyset).getId());
				if (verbose) {
					Interfaz.printAll("\nSe ha asignado el ID:" + mapPlayers.get(keyset).getId() + ", al agente: "
							+ mapPlayers.get(keyset).getName() + "\n");
				}

				send(playerInf);

			}
			step = 1;

		}

		// FASE 1
		// Funcion encargada de la solicitud a los distintos agentes del numero
		// de fichas que estan escondiendo

		private void reqCoins() {

			howManyInRound = 0;
			for (AID keyset : mapPlayers.keySet()) {
				if (mapPlayers.get(keyset).getWon() == false) {
					howManyInRound++;
					ACLMessage playerInf = new ACLMessage(ACLMessage.REQUEST);
					playerInf.addReceiver(mapPlayers.get(keyset).getAid());

					String tmp = "";
					for (AID keyset2 : mapPlayers.keySet()) {
						if (mapPlayers.get(keyset2).getWon() == false) {
							tmp = tmp + mapPlayers.get(keyset2).getId() + ",";
						}
					}
					tmp = tmp.substring(0, tmp.length() - 1);
					playerInf.setContent("GetCoins#" + tmp + "#" + howManyInRound);
					if (verbose) {
						Interfaz.printAll("\nEl agente principal envia: GetCoins#" + tmp + "#" + howManyInRound + " a "
								+ mapPlayers.get(keyset).getName() + "\n");

					}

					send(playerInf);

					ACLMessage message = blockingReceive();

					if (message != null) {

						if (message.getContent().startsWith("MyCoins")
								&& message.getPerformative() == ACLMessage.INFORM) {
							if (verbose) {
								Interfaz.printAll("\nEl agente: " + message.getSender().getName().split("@")[0]
										+ " me indica que ha escondido "
										+ Integer.parseInt(message.getContent().split("#")[1]) + " monedas. (Mensaje:"
										+ message.getContent() + ")\n");
							}
							mapCoins.put(message.getSender().getName().split("@")[0],
									Integer.parseInt(message.getContent().split("#")[1]));

						}

					} else {
						block();
					}

				}
			}
			step = 3;
		}

		// FASE 2
		// Funcion empleada anteriormente en otra version del mainAgent.

		private void recCoins() {
			int contador = 0;

			while (contador < howManyInRound) {
				ACLMessage message = receive();
				if (message != null) {

					if (message.getContent().startsWith("MyCoins") && message.getPerformative() == ACLMessage.INFORM) {
						if (verbose) {
							Interfaz.printAll("\nEl agente: " + message.getSender().getName().split("@")[0]
									+ " me indica que ha escondido "
									+ Integer.parseInt(message.getContent().split("#")[1]) + " monedas. (Mensaje:"
									+ message.getContent() + ")\n");
						}
						mapCoins.put(message.getSender().getName().split("@")[0],
								Integer.parseInt(message.getContent().split("#")[1]));

						contador++;
					}

				} else {
					block();
				}
			}

			step = 3;

		}

		// FASE 3
		// Funcion encargada de la solicitud a los distintos agentes de su
		// apuesta

		private void reqGuessCoins() {

			for (AID keyset : mapPlayers.keySet()) {
				if (mapPlayers.get(keyset).getWon() == false) {

					String tmp = "";
					ACLMessage playerInf = new ACLMessage(ACLMessage.REQUEST);
					playerInf.addReceiver(mapPlayers.get(keyset).getAid());

					for (String keyset2 : mapBets.keySet()) {
						tmp = tmp + mapBets.get(keyset2).intValue() + ",";
					}
					if (!tmp.equals("")) {
						tmp = tmp.substring(0, tmp.length() - 1);
					}

					playerInf.setContent("GuessCoins#" + tmp);
					if (verbose) {
						Interfaz.printAll("\nEl agente principal envia: GuessCoins#" + tmp + "\n");
					}

					send(playerInf);

					ACLMessage message = blockingReceive();

					if (message.getContent().startsWith("MyBet") && message.getPerformative() == ACLMessage.INFORM) {
						if (verbose) {
							Interfaz.printAll(
									"\nRecibo un mensaje de apuesta de: " + message.getSender().getName().split("@")[0]
											+ " de " + Integer.parseInt(message.getContent().split("#")[1])
											+ " monedas. (Mensaje:" + message.getContent() + ")\n");
						}

						mapBets.put(message.getSender().getName().split("@")[0],
								Integer.parseInt(message.getContent().split("#")[1]));

					} else {
						System.out.println(
								"El mensaje recibido no ha sido acorde con lo esperado desde los agentes externos");
					}

				}

			}

			step = 4;

		}

		// FASE 4
		// Funcion encargada de determinar quien ha sido el ganador. Se utiliza
		// para el envio de resultados y calculos internos.

		private void whoWins() {

			int resulSuma = 0;
			String idWinner = "";
			String number_idWinner = "";

			for (String keyset2 : mapCoins.keySet()) {
				resulSuma = resulSuma + mapCoins.get(keyset2).intValue();

			}

			for (String x : mapBets.keySet()) {
				if (mapBets.get(x).intValue() == resulSuma) {

					idWinner = x;

					huboGanador = true;
					Winner = x;

					break;
				} else {
					huboGanador = false;
				}
			}

			if (huboGanador == true) {

				for (AID keyset : mapPlayers.keySet()) {

					if (idWinner.equals(mapPlayers.get(keyset).getName())) {
						number_idWinner = String.valueOf(mapPlayers.get(keyset).getId());

					}
				}

			}

			for (AID keyset4 : mapPlayers.keySet()) {
				if (mapPlayers.get(keyset4).getWon() == false) {
					String tmp = "";
					ACLMessage playerInf = new ACLMessage(ACLMessage.INFORM);
					playerInf.addReceiver(keyset4);
					tmp = "Result#" + number_idWinner + "#" + resulSuma + "#";
					for (String keyset3 : mapBets.keySet()) {
						tmp = tmp + mapBets.get(keyset3).intValue() + ",";
					}
					tmp = tmp.substring(0, tmp.length() - 1);
					tmp = tmp + "#";
					for (String keyset5 : mapCoins.keySet()) {
						tmp = tmp + mapCoins.get(keyset5).intValue() + ",";
					}
					tmp = tmp.substring(0, tmp.length() - 1);

					playerInf.setContent(tmp);
					if (verbose) {
						Interfaz.printAll("\nEl agente principal envia: " + tmp + " a "
								+ mapPlayers.get(keyset4).getName() + "\n");
					}

					send(playerInf);
				}
			}

			if (verbose) {
				Interfaz.printAll("\n-------------------------------------\n");
				Interfaz.printAll("\nHa finalizado esta ronda\n");
				Interfaz.printAll("\n-------------------------------------\n");
			}
			step = 5;

		}

		// FASE 5
		// Funcion encargada de la actualizacion de los parametros internos
		// acorde a la finalizacion de la partida, ademas de las notificaciones
		// correspondientes por pantalla

		public void actualizaResul() {

			Interfaz.actualizaPantalla();
			int contadorLoser = 0;

			if (huboGanador == true) {
				if (verbose) {
					Interfaz.printWinner(Winner);
				}

				for (AID keyset4 : mapPlayers.keySet()) {

					if ((mapPlayers.get(keyset4).getName()).equals(Winner)) {
						mapPlayers.get(keyset4).setWon(true);
						mapPlayers.get(keyset4).setWins(mapPlayers.get(keyset4).getWins() + 1);
						mapPlayers.get(keyset4).setGames(mapPlayers.get(keyset4).getGames() + 1);

					} else {

					}

				}
				for (AID keyset4 : mapPlayers.keySet()) {
					if (mapPlayers.get(keyset4).getWon() == false) {
						contadorLoser++;
					}
				}
				if (contadorLoser == 1) {
					for (AID keyset4 : mapPlayers.keySet()) {
						if (mapPlayers.get(keyset4).getWon() == false) {
							mapPlayers.get(keyset4).setLoses(mapPlayers.get(keyset4).getLoses() + 1);
							mapPlayers.get(keyset4).setGames(mapPlayers.get(keyset4).getGames() + 1);
							if (verbose) {
								Interfaz.printLastLosers(mapPlayers.get(keyset4).getName());
							}

						}
					}
				} else {

				}

			} else {
				if (verbose) {
					Interfaz.printLosers();
				}
			}

			mapCoins.clear();
			mapBets.clear();

			if (contadorLoser == 1) {
				totalGames++;
				for (AID keyset4 : mapPlayers.keySet()) {
					mapPlayers.get(keyset4).setWon(false);

				}

				LinkedHashMap<AID, psi3_player> helpmapPlayers;
				helpmapPlayers = new LinkedHashMap<AID, psi3_player>();
				int cont1 = 0;

				for (AID keyset5 : mapPlayers.keySet()) {
					if (cont1 >= 1) {
						helpmapPlayers.put(mapPlayers.get(keyset5).getAid(), mapPlayers.get(keyset5));
					}
					cont1++;
				}
				cont1 = 1;
				for (AID keyset6 : mapPlayers.keySet()) {
					if (cont1 == 1) {
						helpmapPlayers.put(mapPlayers.get(keyset6).getAid(), mapPlayers.get(keyset6));
					}
					cont1++;
				}

				mapPlayers.clear();

				for (AID keyset7 : helpmapPlayers.keySet()) {
					mapPlayers.put(helpmapPlayers.get(keyset7).getAid(), helpmapPlayers.get(keyset7));
				}

			} else {

				step = 1;
			}

			if (contadorLoser == 1 && totalGames == maxGames) {

				Interfaz.actualizaLastPantalla();
				step = 6;

			} else {
				step = 1;

			}
			Interfaz.cargaTabla();
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
	}

	// Clase correspondiente a la Interfaz Grafica

	class psi3_GUI_panel extends JFrame implements ActionListener, WindowListener, ItemListener {
		private static final long serialVersionUID = 1L;

		private DefaultTableModel defTable = new DefaultTableModel();
		private JTable table = new JTable(defTable);
		private JScrollPane JScrollP = new JScrollPane(table);
		private JTextArea oTAreaLog = new JTextArea();
		private JTextArea oTAreaMat = new JTextArea();
		private JScrollPane oJScrollLog = new JScrollPane(oTAreaLog);
		private JScrollPane oJScrollMat = new JScrollPane(oTAreaMat);
		private JTextField nRounds, sMatrix, nIterations, perCent;
		private psi3_MainAg mainAgent;

		// Constructor de la GUI

		public psi3_GUI_panel(psi3_MainAg mainAgent) {
			super("MENU JUEGO");
			this.mainAgent = mainAgent;
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			addWindowListener(this);

			JMenuBar oMB = new JMenuBar();
			JMenu oMenu = new JMenu("Options");
			oMenu.setFont(new Font("System", Font.ITALIC, 14));
			JMenuItem oMI = new JMenuItem("Exit");
			oMI.setFont(new Font("System", Font.ITALIC, 12));
			oMI.setAccelerator(KeyStroke.getKeyStroke('E', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			oMI.addActionListener(this);
			oMenu.add(oMI);
			oMB.add(oMenu);

			oMenu = new JMenu("Run");
			oMenu.setFont(new Font("System", Font.ITALIC, 14));
			oMI = new JMenuItem("New");
			oMI.setFont(new Font("System", Font.ITALIC, 12));
			oMI.addActionListener(this);
			oMenu.add(oMI);
			oMI = new JMenuItem("Stop");
			oMI.setFont(new Font("System", Font.ITALIC, 12));
			oMI.addActionListener(this);
			oMenu.add(oMI);
			oMI = new JMenuItem("Continue");
			oMI.setFont(new Font("System", Font.ITALIC, 12));
			oMI.addActionListener(this);
			oMenu.add(oMI);
			oMI = new JMenuItem("Number of games");
			oMI.setFont(new Font("System", Font.ITALIC, 12));
			oMI.addActionListener(this);
			oMenu.add(oMI);
			oMB.add(oMenu);

			oMenu = new JMenu("Window");
			oMenu.setFont(new Font("System", Font.ITALIC, 14));
			JCheckBoxMenuItem oCBMI = new JCheckBoxMenuItem("Verbose On (Default:Off)", false);
			oCBMI.setFont(new Font("System", Font.ITALIC, 12));
			oCBMI.addItemListener(this);
			oMenu.add(oCBMI);

			// Eliminar mensajes de la pantalla

			oMI = new JMenuItem("Reset Verbose Information");
			oMI.setFont(new Font("System", Font.ITALIC, 12));
			oMI.addActionListener(this);
			oMenu.add(oMI);
			oMB.add(oMenu);

			oMenu = new JMenu("Help");
			oMenu.setFont(new Font("System", Font.ITALIC, 14));
			oMI = new JMenuItem("About");
			oMI.setFont(new Font("System", Font.BOLD, 12));
			oMI.addActionListener(this);
			oMenu.add(oMI);
			oMB.add(oMenu);
			setJMenuBar(oMB);

			// Ahora creo el JPanel principal, en el que agrupo todo

			JPanel mainPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbConstraints = new GridBagConstraints();
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbConstraints.weightx = 1;
			gbConstraints.weighty = 0.7;

			// TextArea

			gbConstraints.gridy = 0;
			gbConstraints.gridx = 0;
			((DefaultCaret) oTAreaMat.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			oTAreaMat.setEditable(false);
			oTAreaMat.setBackground(Color.white);
			gbConstraints.weighty = 0.8;
			mainPanel.add(oJScrollMat, gbConstraints);

			// TextArea para el modo verbose (Informacion de juego)

			gbConstraints.gridy = 1;
			((DefaultCaret) oTAreaLog.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			oTAreaLog.setEditable(false);
			oTAreaLog.setBackground(Color.white);
			oTAreaLog.append(
					" Se mostrara informacion de juego y comentarios si los comentarios a lo largo del juego estan activados (Recuerde que por defecto estan desactivados)\n");
			gbConstraints.weighty = 0.3;
			mainPanel.add(oJScrollLog, gbConstraints);

			// Aqui muestro las distintas estadisticas de los jugadores

			defTable.setColumnIdentifiers(
					new String[] { "Clasificacion", "ID", "Name", "Player", "Estadisticas(VICT/DERR/PART)" });
			DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
			centerRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
			for (int i = 0; i < defTable.getColumnCount(); i++) {
				table.getColumn(defTable.getColumnName(i)).setCellRenderer(centerRenderer);
			}
			table.setPreferredScrollableViewportSize(table.getPreferredSize());
			table.setFillsViewportHeight(true);
			table.setEnabled(false);
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 2;
			gbConstraints.weighty = 0.5;
			gbConstraints.insets = new Insets(15, 4, 5, 0);
			mainPanel.add(JScrollP, gbConstraints);
			mainAgent.findPlayers();

			gbConstraints.gridy = 3;
			gbConstraints.gridx = 0;
			gbConstraints.weighty = 0;

			setContentPane(mainPanel);
			pack();
			setExtendedState(MAXIMIZED_BOTH);
			setLocationRelativeTo(null);
			setVisible(true);
		}

		// Funciones empleadas desde el programa principal para la gestion desde
		// fuera de los datos mostrados en la interfaz grafica

		public void actualizaPantalla() {
			if (verbose) {
				oTAreaMat.append("\tNombre de los jugadores totales\n\n");
				for (AID keyset4 : mapPlayers.keySet()) {
					oTAreaMat.append("\t" + mapPlayers.get(keyset4).getName());
					oTAreaMat.append("\n");
				}
				oTAreaMat.append("\n\tNombre de los jugadores que quedan en la ronda actual\n\n");
				for (AID keyset4 : mapPlayers.keySet()) {
					if (mapPlayers.get(keyset4).getWon() == false) {
						oTAreaMat.append("\t" + mapPlayers.get(keyset4).getName());
						oTAreaMat.append("\n");
					}
				}
				oTAreaMat.append("\n\tNumero de juegos completados: " + totalGames
						+ "\n\t-------------------------------------\n\n");
			}
		}

		public void actualizaLastPantalla() {
			if (verbose) {
				oTAreaMat.append(
						"\tHa finalizado la simulacion de los juegos indicados, puede consultar los resultados finales en la parte inferior de la pantalla\n\n");
				oTAreaMat.append("\tNumero de juegos completados: " + totalGames + "\n");
				oTAreaMat.append("\n\t-------------------------------------\n");

			}
		}

		public void printWinner(String winner) {
			oTAreaLog.append("CONGRATULATIONS " + winner + ", YOU ARE THE WINNER.\n");
		}

		public void printLosers() {
			oTAreaLog.append("THERE HAS BEEN NO WINNER. Try Again!\n");
		}

		public void printLastLosers(String loser) {
			oTAreaLog.append("THE LOSER IN THIS GAME WAS " + loser + ", DO IT BETTER NEXT TIME!\n");
		}

		public void printAll(String x) {
			oTAreaLog.append(x);
		}

		public void cargaTabla() {

			ArrayList<psi3_player> playerList = mainAgent.findPlayersArray();
			Collections.sort(playerList, new Comparator<psi3_player>() {
				// Ordena por victorias
				@Override
				public int compare(psi3_player o1, psi3_player o2) {
					int result = new Integer(o2.getWins()).compareTo(new Integer(o1.getWins()));
					if (result == 0) {
						return new Integer(o2.getWins()).compareTo(new Integer(o1.getWins()));
					} else {
						return result;
					}
				}
			});

			int rowToErase = defTable.getRowCount();

			for (int j = 0; j < rowToErase; j++) {

				defTable.removeRow(0);
			}
			for (int i = 0; i < playerList.size(); i++) {
				String winrate = playerList.get(i).getWins() + "/" + playerList.get(i).getLoses() + "/"
						+ playerList.get(i).getGames();
				defTable.addRow(new String[] { Integer.toString(i + 1), playerList.get(i).getIdString(),
						playerList.get(i).getName(), playerList.get(i).getType(), winrate });
			}
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				if (((JCheckBoxMenuItem) e.getSource()).getActionCommand().equals("Verbose On (Default:Off)")) {
					verbose = true;
				}
			} else if (e.getStateChange() == ItemEvent.DESELECTED) {
				if (((JCheckBoxMenuItem) e.getSource()).getActionCommand().equals("Verbose On (Default:Off)")) {
					verbose = false;
				}
			}
		}

		public void actionPerformed(ActionEvent e) {
			if ("Exit".equals(e.getActionCommand()))
				System.exit(0);
			else if ("About".equals(e.getActionCommand())) {
				String sAux[] = { "PSI Practica B", "Jose Fernandez Gomez", "PSI-3" };
				new psi3_DialogOK(this, "About", true, sAux, true);
			} else if ("Number of games".equals(e.getActionCommand())) {

				vNumberofGames();
				new psi3_Dialogo(this, "Number of games", true, Integer.toString(maxGames));
			} else if ("Reset Verbose Information".equals(e.getActionCommand()))
				vResetVerboseInformation();
			else if ("New".equals(e.getActionCommand())) {

				vNew();
				mainAgent.New();
			} else if ("Stop".equals(e.getActionCommand())) {
				stop = true;
				vStop();
				mainAgent.Stop();
			} else if ("Continue".equals(e.getActionCommand())) {
				stop = false;
				vContinue();
				mainAgent.Continue();

			}
		}

		private void vContinue() {
			oTAreaLog.append("\n-------------------------------------\nSe ha pulsado el boton Continue\n");
			oTAreaLog.append("Continua la ejecucion del juego actual\n-------------------------------------\n");
		}

		private void vStop() {
			oTAreaLog.append("\n-------------------------------------\nSe ha pulsado el boton Stop\n");
			oTAreaLog.append("Parada la ejecucion del juego actual\n-------------------------------------\n");
		}

		private void vNew() {
			oTAreaLog.append("\n-------------------------------------\nSe ha pulsado el boton New\n");
			oTAreaLog.append("Hemos iniciado una nueva partida\n-------------------------------------\n");

		}

		private void vResetVerboseInformation() {
			oTAreaLog.setText("\n-------------------------------------\n");
			oTAreaLog.setText("Se ha limpiado la pantalla");
			oTAreaLog.setText("\n-------------------------------------\n");
		}

		private void vNumberofGames() {
			oTAreaLog.append("\n-------------------------------------\nSe ha pulsado el boton Number of Games\n");
			oTAreaLog.append("El numero de juegos en la partida anterior: " + totalGames);
			oTAreaLog.append("\n-------------------------------------\n");
		}

		@Override
		public void windowActivated(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowClosed(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowClosing(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeactivated(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeiconified(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowIconified(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowOpened(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}
	}

	class psi3_DialogOK extends Dialog implements ActionListener {
		private static final long serialVersionUID = 1L;

		public psi3_DialogOK(JFrame oPadre, String sTit, boolean bBool, String sCad[], boolean bCentrado) {
			super(oPadre, sTit, bBool);
			setBackground(Color.lightGray);
			setForeground(Color.black);
			if (bCentrado) {
				setLayout(new GridLayout(sCad.length + 4, 1));
				add(new JLabel("", JLabel.CENTER));
				for (int i = 0; i < sCad.length; i++)
					add(new JLabel(sCad[i], JLabel.CENTER));
				add(new JLabel("", JLabel.CENTER));
				JPanel oPanel = new JPanel();
				oPanel.setLayout(new GridLayout(1, 3));
				oPanel.add(new JLabel("", JLabel.CENTER));
				JButton oOK = new JButton("OK");
				oOK.addActionListener(this);
				oPanel.add(oOK);
				oPanel.add(new JLabel("", JLabel.CENTER));
				add(oPanel);
				add(new JLabel("", JLabel.CENTER));
				setSize(new Dimension(300, 50 + 70 * sCad.length));
			} else {
				setLayout(new GridLayout(sCad.length / 2 + 4, 1));
				add(new JLabel("", JLabel.CENTER));

				JPanel oPanel;
				for (int i = 0; i < sCad.length; i += 2) {
					oPanel = new JPanel();
					oPanel.setLayout(new GridLayout(1, 2));
					oPanel.add(new JLabel(sCad[i], JLabel.LEFT));
					oPanel.add(new JLabel(sCad[i + 1], JLabel.LEFT));
					add(oPanel);
				}
				add(new JLabel("", JLabel.CENTER));
				oPanel = new JPanel();
				oPanel.setLayout(new GridLayout(1, 3));
				oPanel.add(new JLabel("", JLabel.CENTER));
				JButton oOK = new JButton("OK");
				oOK.addActionListener(this);
				oPanel.add(oOK);
				oPanel.add(new JLabel("", JLabel.CENTER));
				add(oPanel);
				add(new JLabel("", JLabel.CENTER));
				setSize(new Dimension(300, 50 + 70 * sCad.length / 2));
			}
			setLocationRelativeTo(null);
			setVisible(true);
		}

		public void actionPerformed(ActionEvent evt) {
			if ("OK".equals(evt.getActionCommand()))
				dispose();
		}
	}

	class psi3_Dialogo extends Dialog implements ActionListener {
		private static final long serialVersionUID = 1L;
		public JTextField oTF;

		psi3_Dialogo(JFrame oPadre, String sTit, boolean bBool, String sCad) {
			super(oPadre, sTit, bBool);

			setBackground(Color.lightGray);
			setForeground(Color.black);

			setLayout(new GridLayout(2, 1));

			oTF = new JTextField(sCad, 20);
			add(oTF);

			JPanel oPanel = new JPanel();
			oPanel.setLayout(new GridLayout(1, 2));
			JButton oBut = new JButton("OK");
			oBut.addActionListener(this);
			oPanel.add(oBut);
			oBut = new JButton("Cancel");
			oBut.addActionListener(this);
			oPanel.add(oBut);
			add(oPanel);

			setSize(new Dimension(300, 150));
			setLocation(new Point(150, 150));
			setVisible(true);
		}

		public void actionPerformed(ActionEvent evt) {
			if ("OK".equals(evt.getActionCommand())) {
				maxGames = Integer.parseInt(oTF.getText());
				dispose();
			}

			else if ("Cancel".equals(evt.getActionCommand())) {
				oTF.setText("");
				dispose();
			}
		}

	}

}

class psi3_player {
	private int id;
	private String name;
	private String type = "Player";
	private AID aid;
	private int wins;
	private int loses;
	private int games;
	private boolean won;
	private int orden;

	public psi3_player(int id, String name, String type, AID aid, int wins, int loses, int games, boolean won) {
		this.setId(id);
		this.setName(name);
		this.setType(type);
		this.aid = aid;
		this.wins = wins;
		this.loses = loses;
		this.games = games;
		this.won = false;
	}

	public boolean getWon() {
		return won;
	}

	public void setWon(boolean won) {
		this.won = won;
	}

	public int getWins() {
		return wins;
	}

	public void setWins(int wins) {
		this.wins = wins;
	}

	public int getLoses() {
		return loses;
	}

	public void setLoses(int loses) {
		this.loses = loses;
	}

	public int getGames() {
		return games;
	}

	public void setGames(int games) {
		this.games = games;
	}

	public String getIdString() {

		return Integer.toString(id);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public AID getAid() {
		return aid;
	}

	public void setAid(AID aid) {
		this.aid = aid;
	}
}
