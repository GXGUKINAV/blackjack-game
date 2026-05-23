Blackjack V6 – Gioca, Scommetti e Vinci <br>
<img src="Blackjack/web/logo/Blackjack_yellow.png" alt="Blackjack V6 Interface" style="width: 5%; max-width: 400px;">

Blackjack V6 è un gioco di carte completo basato sul classico Blackjack, trasformato in un'applicazione web moderna con autenticazione, scommesse e statistiche personali. Gioca direttamente dal browser, gestisci il tuo saldo di crediti e segui i tuoi progressiourney.

Cosa fa il gioco
🎰 Gioca a Blackjack classico: Chiedi carta (Hit) o ferma (Stand) per raggiungere 21 senza sballare, superando il dealer

💰 Sistema di scommesse: Punta da 10 a 100 chips per partita, con blackjack naturale che paga 3:2

🔐 Account personali: Registrati e accedi per salvare i tuoi crediti e le tue partite nel database

📊 Statistiche dettagliate: Visualizza partite totali, vittorie, sconfitte, win rate e storico 

🎨 Interfaccia moderna: Carte con effetto glossy hover, navigazione SPA, chips decorative con logo ufficiale

Caratteristiche principali
Funzionalità	Descrizione
Autenticazione	Login e registrazione con sessioni sicure (token UUID)
Crediti persistenti	1000 chips all'iscrizione, saldi aggiornati in tempo reale nel database MySQL
Scommesse dinamiche	Puntate da 10-100 chips, incrementi di 10, adattamento automatico se i crediti scendono
SPA completa	Tre schermate: Login, Gioco, Statistiche con navbar di navigazione
Database MySQL	Salvataggio persistente di giocatori, partite e participationi con esito dettagliato
Effetti grafici	Carte con riflesso olografico stile iOS al passaggio del mouse
Evoluzione del progetto
Il gioco è partito da una semplice versione console (V1) ed è evoluto attraverso 6 versioni maggiori:

V1-V2: Gioco base in console e client-server locale

V3-V3.2: Interfaccia web HTML/CSS/JS con carte grafiche

V4-V4.1: Carte complete in immagini, effetti glossy, gestione stato partita

V5: Integrazione database MySQL per persistenza dati

V6-V6.2: Login, scommesse, statistiche e navigazione multi-schermata completi, Maintenance leggera/Bug fixes e Responsive Mobile e Fix Logica Dealer

Come iniziare
Avvia il server Java (backend HTTP su localhost:6767)

Apri il browser su http://localhost:6767

Registrati con 1000 chips iniziali o accedi al tuo account

Imposta la tua scommessa e clicca Start

Gioca usando Hit o Stand, guarda le statistiche crescere!

Il progetto mantiene una struttura pulita tra logica di gioco, API REST, database e interfaccia grafica, pronto per futuro sviluppo.

Versione corrente: V6.1.0 - Stack: Java + MySQL + HTML/CSS/JS
