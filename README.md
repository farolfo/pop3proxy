PROXY POP3
-----------------------------------------------------------------------
Desarrollado para la catedra de Protocolos de Comunicacion del ITBA junto a Jorge Mozzino y Tomas Mehdi.

Compilacion
-----------------------------------------------------------------------
	Para la compilacion se ejecuta 

	$> ant compile

-----------------------------------------------------------------------
Ejecucion

	Para correrlo se ejecuta java -jar con el path del jar "proxy.jar" generado en ~build/jar.
	
	Soporta los parametroe -p -d -c siendo estos, respectivamente, source port, destination port y el puerto de configuracion remota. 

	Si -p no se asigna toma por default 3000, si -d no se asigna
	toma por default 110, si -c no se asigna toma por default 51914

-----------------------------------------------------------------------
Configuracion inicial

  Para configurar su proxy usted debe configurar el archivo "conf.xml", el cual se debe encontrar en el directorio donde se encuentra la aplicacion.

  La configuracion minima necesaria para el funcionamiento del proxy requiere que se setee un pop3 server default en el archivo de configuracion.

  Por ejemplo, podriamos tener:

  <proxyServerConfiguration>
	<defaultServer>pop3.itba.edu.ar</defaultServer>
  </proxyServerConfiguration>

Administradores (configuracion remota):

  Para agregar administradores(personas autorizadas a configurar el proxy remotamente), se deben especificar de la siguiente manera:
 
  <proxyServerConfiguration>
	<defaultServer>pop3.itba.edu.ar</defaultServer>
      	<administrators>
		<administrator>
			<username>jperez</username>
			<password>123465</password>
		</administrator>
		<administrator>
			<username>sgomez</username>
			<password>123465</password>
		</administrator>
	<administrators>  
  </proxyServerConfiguration>

Preferencias de usuarios:
   
   Se muestra a continuacion un ejemplo con las preferencias de usuario.

   <proxyServerConfiguration>
	<defaultServer>pop3.itba.edu.ar</defaultServer>
      <user>
		<username>farolfo</username>
		<server>pop3.alu.itba.edu.ar</server>
		<timesToLogin>
			<!-- Se indican intervalos a loguearse en formato 1 a 24 -->
			<hourFrom>3</hourFrom>
			<hourTo>6</hourTo>
		</timesToLogin>
		<timesToLogin>
			<!-- Se indican intervalos a  -->
			<hourFrom>1</hourFrom>
			<hourTo>2</hourTo>
		</timesToLogin>
		<countLoginsPerDay>5</countLoginsPerDay>
		<restrictions>
			<!-- Basado en la antiguedad -->
			<cantDays>50</cantDays> 
			
			<!-- Basado en el remitente del correo -->			
			<from>
				<email>francoarolfo@hotmail.com</email>
				<username>francoarolfo</username>
			</from>

			<!-- Basado en el Content-Type de sus partes -->
			<type>image</type>

			<!-- Basado en el tamano del contenido -->
			<size>400</size>

			<!-- Basado en algun patron sobre cabeceras -->
			<headerPattern>
				<header>"header"</header>
				<pattern>"pattern"</pattern>
			<headerPattern>

			<!-- Basado en la estructura del mensaje, solo soporta "attachments" o "no attachments" -->
			<messageStructure>attachments<messageStructure>	
		</restrictions>
	</user>
  </proxyServerConfiguration>

Restricciones globales de borrado:

   Se aplicaran a todos los mails a borrar.

	<proxyServerConfiguration>
		<defaultServer>pop3.itba.edu.ar</defaultServer>
		<globalDelRestrictions>
			<!-- Basado en la antiguedad -->
			<cantDays>50</cantDays> 
			
			<!-- Basado en el remitente del correo -->			
			<from>
				<email>francoarolfo@hotmail.com</email>
				<username>francoarolfo</username>
			</from>

			<!-- Basado en el Content-Type de sus partes -->
			<type>image</type>

			<!-- Basado en el tamano del contenido -->
			<size>400</size>

			<!-- Basado en algun patron sobre cabeceras -->
			<headerPattern>
				<header>"header"</header>
				<pattern>"pattern"</pattern>
			<headerPattern>

			<!-- Basado en la estructura del mensaje, solo soporta "attachments" o "no attachments" -->
			<messageStructure>attachments<messageStructure>
		</globalDelRestrictions>
       	</proxyServerConfiguration>


Restriccion de IPs:

  Podemos restringir IPs de 4 maneras distintas: por direccion IP, por hostname(se resuelve por DNS), por subred(formato cidr) o por subred(formato direccion y submascara).

  <proxyServerConfiguration>
	<defaultServer>pop3.itba.edu.ar</defaultServer>
        <restrictedIps>
		<ip>125.2.2.6</ip>
		<ip>4.2.6.3</ip>
		<hostname>www.itba.edu.ar</hostname>
		<subnet_cidr>125.6.6.0/24</subnet_cidr>
		<subnet>
			<address>125.2.0.0</address>
			<submask>255.255.0.0</submask>
		</subnet>
	</restrictedIps>
  </proxyServerConfiguration>

Transformacion a los mails:

  Se permite setear solo una transformacion. Se debe indicar el path del programa.

  <proxyServerConfiguration>
	<defaultServer>pop3.itba.edu.ar</defaultServer>
        <transformation>path</transformation>
  </proxyServerConfiguration>