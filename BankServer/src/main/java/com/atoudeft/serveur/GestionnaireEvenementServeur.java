package com.atoudeft.serveur;

import com.atoudeft.banque.Banque;
import com.atoudeft.banque.CompteEpargne;
import com.atoudeft.banque.Operation;
import com.atoudeft.banque.TypeCompte;
import com.atoudeft.banque.serveur.ConnexionBanque;
import com.atoudeft.banque.serveur.ServeurBanque;
import com.atoudeft.commun.evenement.Evenement;
import com.atoudeft.commun.evenement.GestionnaireEvenement;
import com.atoudeft.commun.net.Connexion;

/**
 * Cette classe représente un gestionnaire d'événement d'un serveur. Lorsqu'un serveur reçoit un texte d'un client,
 * il crée un événement à partir du texte reçu et alerte ce gestionnaire qui réagit en gérant l'événement.
 *
 * @author Abdelmoumène Toudeft (Abdelmoumene.Toudeft@etsmtl.ca)
 * @version 1.0
 * @since 2023-09-01
 */
public class GestionnaireEvenementServeur implements GestionnaireEvenement {
    private Serveur serveur;

    /**
     * Construit un gestionnaire d'événements pour un serveur.
     *
     * @param serveur Serveur Le serveur pour lequel ce gestionnaire gère des événements
     */
    public GestionnaireEvenementServeur(Serveur serveur) {
        this.serveur = serveur;
    }

    /**
     * Méthode de gestion d'événements. Cette méthode contiendra le code qui gère les réponses obtenues d'un client.
     *
     * @param evenement L'événement à gérer.
     */
    @Override
    public void traiter(Evenement evenement) {
        Object source = evenement.getSource();
        ServeurBanque serveurBanque = (ServeurBanque)serveur;
        Banque banque;
        ConnexionBanque cnx;
        String msg, typeEvenement, argument, numCompteClient, nip;
        String[] t;

        if (source instanceof Connexion) {
            cnx = (ConnexionBanque) source;
            System.out.println("SERVEUR: Recu : " + evenement.getType() + " " + evenement.getArgument());
            typeEvenement = evenement.getType();
            cnx.setTempsDerniereOperation(System.currentTimeMillis());
            switch (typeEvenement) {
                /******************* COMMANDES GÉNÉRALES *******************/
                case "CONNECT":
                    argument = evenement.getArgument();
                    t = argument.split(":");
                    numCompteClient = t[0];
                    nip = t[1];

                    banque = serveurBanque.getBanque();
                    //vérification de si ce compte est déja connectée dans une autre instance de BankClient
                    for(Connexion cnected: serveur.connectes) {

                        if (cnected instanceof ConnexionBanque && ((ConnexionBanque) cnected).getNumeroCompteClient() == null){
                            cnx.envoyer("CONNECT NO");
                        }


                    }
                    //vérification des credentiels envoyés
                    if(banque.getCompteClient(numCompteClient) != null){
                        if(banque.getCompteClient(numCompteClient).getNip().matches(nip)){
                            cnx.envoyer("CONNECT OK");
                            cnx.setNumeroCompteClient(numCompteClient);
                        }
                    }

                    //
                    break;
                case "EXIT": //Ferme la connexion avec le client qui a envoyé "EXIT":
                    cnx.envoyer("END");
                    serveurBanque.enlever(cnx);
                    cnx.close();
                    break;
                case "LIST": //Envoie la liste des numéros de comptes-clients connectés :
                    cnx.envoyer("LIST " + serveurBanque.list());
                    break;
                /******************* COMMANDES DE GESTION DE COMPTES *******************/
                case "NOUVEAU": //Crée un nouveau compte-client :
                    if (cnx.getNumeroCompteClient()!=null) {
                        cnx.envoyer("NOUVEAU NO deja connecte");
                        break;
                    }
                    argument = evenement.getArgument();
                    t = argument.split(":");
                    if (t.length<2) {
                        cnx.envoyer("NOUVEAU NO");
                    }
                    else {
                        numCompteClient = t[0];
                        nip = t[1];
                        banque = serveurBanque.getBanque();
                        if (banque.ajouter(numCompteClient,nip)) {
                            cnx.setNumeroCompteClient(numCompteClient); // on se connect quand on cree un compte ?
                            cnx.setNumeroCompteActuel(banque.getNumeroCompteParDefaut(numCompteClient));
                            cnx.envoyer("NOUVEAU OK "+ t[0] +" cree");
                        }
                        else
                            cnx.envoyer("NOUVEAU NO "+t[0]+" existe");
                    }
                    break;
                case "EPARGNE":
                    if(cnx.getNumeroCompteClient()!=null && !serveurBanque.getBanque().verifSiDejaCompte(cnx.getNumeroCompteClient(),TypeCompte.valueOf("EPARGNE"))){
                        banque = serveurBanque.getBanque();
                        //creer on nouv compte ?
                        banque.getCompteClient(cnx.getNumeroCompteClient()).ajouter(new CompteEpargne(banque.getNumCompteBancaireValide(),TypeCompte.EPARGNE,5));
                        //on send un msg ou non ?
                        cnx.envoyer("EPARGNE OK");
                    } else {
                        cnx.envoyer("EPARGNE NO");
                    }
                    break;
                case "SELECT":
                    if(cnx.getNumeroCompteClient()!=null){
                        argument = evenement.getArgument();
                        banque = serveurBanque.getBanque();
                        cnx.setNumeroCompteActuel(banque.getCompteClient(cnx.getNumeroCompteClient()).getNumCompteBancaire(TypeCompte.valueOf(argument)));
                        cnx.envoyer("SELECT OK");
                    } else {
                        cnx.envoyer("SELECT NO");
                    }
                    break;

                case "DEPOT":
                    argument = evenement.getArgument();
                    banque = serveurBanque.getBanque();
                    int comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                    double quantiteAjout = Double.parseDouble(String.format("%.2d",argument));
                    banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).crediter(quantiteAjout);

                    break;

                case "RETRAIT":
                    argument = evenement.getArgument();
                    banque = serveurBanque.getBanque();
                    comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                    double quantiteRetrait = Double.parseDouble(String.format("%.2d",argument));
                    banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).debiter(quantiteRetrait);

                    break;

                case "FACTURE":
                    argument = evenement.getArgument();
                    banque = serveurBanque.getBanque();
                    comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                    t = argument.split("\\s+");// Ce Regex sépare les
                    double montantPaye = Double.parseDouble(String.format("%.2d",t[0]));
                    String numero_facture = t[1];
                    String Description = t[2];

                    banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).payerFacture(numero_facture,montantPaye,Description);

                    break;

                case "TRANSFER" :
                    argument = evenement.getArgument();
                    banque = serveurBanque.getBanque();
                    comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                    t = argument.split("\\s+");// ce REGEX sépare l'argument à chaque espace incluent un tab. Si il y a problème à ce propos, Il est probablement ici.
                    double montantTransfer = Double.parseDouble(String.format("%.2d",t[0]));
                    String cible = t[1];

                    
                    banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).transferer(montantTransfer,cible);

                    break;

                case "HIST":
                    banque = serveurBanque.getBanque();
                    comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                    banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).getHistorique();

                    //pour chaque opérations, Il va imprimer le toString de tout ce qui hérite de Opération.
                    for(Operation op:banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).getHistorique()){
                        cnx.envoyer(op.toString());
                        cnx.envoyer("");
                    }
                    break;
                /******************* TRAITEMENT PAR DÉFAUT *******************/
                default: //Renvoyer le texte recu convertit en majuscules :
                    msg = (evenement.getType() + " " + evenement.getArgument()).toUpperCase();
                    cnx.envoyer(msg);
            }
        }
    }
}