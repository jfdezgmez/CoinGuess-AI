You must include jade.jar to run this agents.

- Para compilar:

	javac -classpath jade.jar *.java
	
Para ejecutar:

	java -classpath jade.jar:. jade.Boot -agents "nombre_agent:nombre_class;..."
	
	por ejemplo:
		
		java -classpath jade.jar:. jade.Boot -agents "MainAg:psi3_MainAg;Random1:psi3_Random;Random2:psi3_Random;Fixed1:psi3_Fixed"

		Esto lanza el main agent y 3 jugadores: dos random y uno fijo.
		
Extra:
		
		
		-Existe un bot�n en el men� "Window" para resetear el TextArea de los comentarios.
		
		-Se han ordenado los jugadores acorde a sus estad�sticas, y luego respecto a ID.


������������������������

Explicaci�n de la inteligencia:

El agente inteligente implementa una inteligencia basada en Q-learning.
Se han diferenciado los distintos estados posibles empleando el par de valores (Monedas escondidas, Numero de jugadores en esa ronda * numero max de monedas(=3)).
Para cada uno de estos estados, tendremos asociado un vector de acciones correspondiente al numero de acciones permitidas en ese momento, que en funci�n de los resultados finales ir� confirmando un vector con unos valores asociados adecu�ndolo en la mayor medida a la acci�n siguiente �ptima a realizar.
En caso de no haber una acci�n siguiente �ptima o no tener datos anteriores de ese par, el agente podr� optar por el descubrimiento de nuevas acciones con el fin de completar su vector.


El ajuste de par�metros ha sido obtenido mediante numerosas pruebas contra agentes del mismo tipo con diferentes par�metros hasta conseguir estos valores.

final static double ALPHA = 0.6;  
final static double dGamma = 0.4;
final static double dEpsilon = 0.2;
final static double dDecFactorLR = 0.99;
final static double dMINLearnRate = 0.3;

Explicaci�n de par�metros :

El Learning Rate (Alpha) comienza en 0.6 y se va decrementando a raz�n 0.99 hasta llegar a un m�nimo 0.3
El Discount Factor(Gamma) vale 0.4.
El Epsilon nos sirve para determinar cu�ntas veces se juega aleatoriamente para descubrir mejores estados, su valor es 20%.

Comentarios respecto al algoritmo: 

Se ha valorado la inclusi�n de un vector de acciones tambi�n para el numero de fichas que esconde el agente, descartando esto por dar malos resultados frente a agentes inteligentes, ya que ten�a un comportamiento similar a los fixed agent en caso de que una elecci�n le hubiese dado buenos resultados repetidas veces.
Tambi�n se ha probado con la inclusi�n de una recompensa(reward en el c�digo) proporcional a la distancia entre mi apuesta y la suma de todas las fichas escondidas por los jugadores(la apuesta correcta), sin embargo despu�s de m�ltiples pruebas, los resultados obtenidos promediaban un orden mas alto de derrotas, as� bien, en un numero largo de partidas obten�an resultados similares.

������������������������

Errores en la ejecuci�n:

En algunas ocasiones, al activar el verbose mode ha saltado una excepci�n de �ArrayOutBounds�� relacionada con la velocidad entre la ejecuci�n de los agentes y la capacidad de la GUI para la recarga de la pantalla.
Se ha intentado solucionar mediante la inclusi�n de un sleep antes del comienzo de una nueva partida, sin embargo y aunque el error se evit� en gran parte de las ocasiones, por momentos y dependiendo de la actuaci�n de los agentes la excepci�n ha vuelto a aparecer. Sin embargo no tiene mayor efecto en el juego ya que las recargas est�n programadas y se puede ver igualmente las estad�sticas en tiempo real.
	