package io.github.toberocat.improvedfactions.modules.streamchat.listener

/*
esto es por websockets entonces necesitamos nginx, no voy a pasar directamente los datos a minecraft server
pondre un proxy por delante, pero cuando el plugin este listo podran hacer lo que quieranm:
Responses: 200, 400, 401, 403, 500
object/json
*/

// https://api.kick.com/public/v1/public-key
// https://id.kick.com <- oauth sevrer
/*
https://id.kick.com/oauth/authorize?
response_type=code&
client_id=<your_client_id>&
redirect_uri=<https://yourapp.com/callback>&
scope=<scopes>&
code_challenge=<code_challenge>&
code_challenge_method=S256&
state=<random_value>

encontre info muy util... pero es mucho para hoy asi que jugare algo...xD

*/
//http requests
//estas variables tienen que estar en privado para prevenir la lectura publica...
class kickAPI {
    private val url = "https://example.com/kick"
    private val method = "POST"
    private val headers = mapOf("Content-Type" to "application/json")
    private val clientID = "example-client-id"
    private val secret = "example-secret"

}



// Nueva funcionalidad kick.com integracion del chat en minecraft:
//ok, hay muchas opciones... pero me voy a limitar a lo util...
//recibir chats en minecraft y poder responder creo que es lo mas util...
/*
    permisos para kick:
    enviar mensajes
    recibir mensajes
 */


//ya con esto creo que tengo para empezar...
