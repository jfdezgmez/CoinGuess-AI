You must include jade.jar to run this agents.

- Para compilar:

	javac -classpath jade.jar *.java
	
Para ejecutar:

	java -classpath jade.jar:. jade.Boot -agents "nombre_agent:nombre_class;..."
	
	por ejemplo:
		
		java -classpath jade.jar:. jade.Boot -agents "MainAg:psi3_MainAg;Random1:psi3_Random;Random2:psi3_Random;Fixed1:psi3_Fixed"

		Esto lanza el main agent y 3 jugadores: dos random y uno fijo.
		
Extra:
		
		
		-Existe un botón en el menú "Window" para resetear el TextArea de los comentarios.
		
		-Se han ordenado los jugadores acorde a sus estadísticas, y luego respecto a ID.


————————————————————————

Explicación de la inteligencia:

El agente inteligente implementa una inteligencia basada en Q-learning.
Se han diferenciado los distintos estados posibles empleando el par de valores (Monedas escondidas, Numero de jugadores en esa ronda * numero max de monedas(=3)).
Para cada uno de estos estados, tendremos asociado un vector de acciones correspondiente al numero de acciones permitidas en ese momento, que en función de los resultados finales irá confirmando un vector con unos valores asociados adecuándolo en la mayor medida a la acción siguiente óptima a realizar.
En caso de no haber una acción siguiente óptima o no tener datos anteriores de ese par, el agente podrá optar por el descubrimiento de nuevas acciones con el fin de completar su vector.


El ajuste de parámetros ha sido obtenido mediante numerosas pruebas contra agentes del mismo tipo con diferentes parámetros hasta conseguir estos valores.

final static double ALPHA = 0.6;  
final static double dGamma = 0.4;
final static double dEpsilon = 0.2;
final static double dDecFactorLR = 0.99;
final static double dMINLearnRate = 0.3;

Explicación de parámetros :

El Learning Rate (Alpha) comienza en 0.6 y se va decrementando a razón 0.99 hasta llegar a un mínimo 0.3
El Discount Factor(Gamma) vale 0.4.
El Epsilon nos sirve para determinar cuántas veces se juega aleatoriamente para descubrir mejores estados, su valor es 20%.

Comentarios respecto al algoritmo: 

Se ha valorado la inclusión de un vector de acciones también para el numero de fichas que esconde el agente, descartando esto por dar malos resultados frente a agentes inteligentes, ya que tenía un comportamiento similar a los fixed agent en caso de que una elección le hubiese dado buenos resultados repetidas veces.
También se ha probado con la inclusión de una recompensa(reward en el código) proporcional a la distancia entre mi apuesta y la suma de todas las fichas escondidas por los jugadores(la apuesta correcta), sin embargo después de múltiples pruebas, los resultados obtenidos promediaban un orden mas alto de derrotas, así bien, en un numero largo de partidas obtenían resultados similares.

————————————————————————

Errores en la ejecución:

En algunas ocasiones, al activar el verbose mode ha saltado una excepción de “ArrayOutBounds…” relacionada con la velocidad entre la ejecución de los agentes y la capacidad de la GUI para la recarga de la pantalla.
Se ha intentado solucionar mediante la inclusión de un sleep antes del comienzo de una nueva partida, sin embargo y aunque el error se evitó en gran parte de las ocasiones, por momentos y dependiendo de la actuación de los agentes la excepción ha vuelto a aparecer. Sin embargo no tiene mayor efecto en el juego ya que las recargas están programadas y se puede ver igualmente las estadísticas en tiempo real.
	