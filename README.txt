
20/01/2018 @jfdezgmez

You must include jade.jar to run this agents.

- Para compilar:

	javac -classpath jade.jar *.java
	
Para ejecutar:

	java -classpath jade.jar:. jade.Boot -agents "nombre_agent:nombre_class;..."
	
	por ejemplo:
		
		java -classpath jade.jar:. jade.Boot -agents "MainAg:psi3_MainAg;Random1:psi3_Random;Random2:psi3_Random;Fixed1:psi3_Fixed"

		Esto lanza el main agent y 3 jugadores: dos random y uno fijo.
		
Extra:
		
		
		-Existe un bot—n en el menu "Window" para resetear el TextArea de los comentarios.
		
		-Se han ordenado los jugadores acorde a sus estadisticas, y luego respecto a ID.


Explicacion de la inteligencia:

El agente inteligente implementa una inteligencia basada en Q-learning.
Se han diferenciado los distintos estados posibles empleando el par de valores (Monedas escondidas, Numero de jugadores en esa ronda * numero max de monedas(=3)).
Para cada uno de estos estados, tendremos asociado un vector de acciones correspondiente al numero de acciones permitidas en ese momento, que en funcion de los resultados finales ira confirmando un vector con unos valores asociados adecuandolo en la mayor medida a la accion siguiente optima a realizar.
En caso de no haber una accion siguiente optima o no tener datos anteriores de ese par, el agente podra optar por el descubrimiento de nuevas acciones con el fin de completar su vector.


El ajuste de parametros ha sido obtenido mediante numerosas pruebas contra agentes del mismo tipo con diferentes parametros hasta conseguir estos valores.

final static double ALPHA = 0.6;  
final static double dGamma = 0.4;
final static double dEpsilon = 0.2;
final static double dDecFactorLR = 0.99;
final static double dMINLearnRate = 0.3;

Explicacion de parametros :

El Learning Rate (Alpha) comienza en 0.6 y se va decrementando a razon 0.99 hasta llegar a un minimo 0.3
El Discount Factor(Gamma) vale 0.4.
El Epsilon nos sirve para determinar cuantas veces se juega aleatoriamente para descubrir mejores estados, su valor es 20%.

Comentarios respecto al algoritmo: 

Se ha valorado la inclusion de un vector de acciones tambien para el numero de fichas que esconde el agente, descartando esto por dar malos resultados frente a agentes inteligentes, ya que tenia un comportamiento similar a los fixed agent en caso de que una eleccion le hubiese dado buenos resultados repetidas veces.
Tambien se ha probado con la inclusion de una recompensa(reward en el codigo) proporcional a la distancia entre mi apuesta y la suma de todas las fichas escondidas por los jugadores(la apuesta correcta), sin embargo despues de multiples pruebas, los resultados obtenidos promediaban un orden mas alto de derrotas, asi bien, en un numero largo de partidas obtenian resultados similares.



Errores en la ejecucion:

En algunas ocasiones, al activar el verbose mode ha saltado una excepcion de 'ArrayOutBounds..' relacionada con la velocidad entre la ejecucion de los agentes y la capacidad de la GUI para la recarga de la pantalla.
Se ha intentado solucionar mediante la inclusion de un sleep antes del comienzo de una nueva partida, sin embargo y aunque el error se evite en gran parte de las ocasiones, por momentos y dependiendo de la actuacion de los agentes la excepcion ha vuelto a aparecer. Sin embargo no tiene mayor efecto en el juego ya que las recargas estan programadas y se puede ver igualmente las estadisticas en tiempo real.
	
