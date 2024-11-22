package com.atoudeft.serveur;

import com.atoudeft.banque.Banque;
import com.atoudeft.banque.CompteEpargne;
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
                    boolean dejaConnecte = false;
                    //On vérifie si le compte existe.
                    if(banque.compteExiste(numCompteClient)){
                        //vérification de si ce compte est déja connectée dans une autre instance de BankClient
                        for(Connexion connected: serveur.connectes) {
                            if(((ConnexionBanque)connected).getNumeroCompteClient() != null){
                                if ((connected instanceof ConnexionBanque && ((ConnexionBanque)connected).getNumeroCompteClient().equals(numCompteClient))) {
                                    System.out.println("deja connecte ailleurs");
                                    cnx.envoyer("CONNECT NO");
                                    dejaConnecte = true;
                                }
                            }
                        }
                        if (!dejaConnecte) {
                            //vérification des credentiels envoyés.
                            if(banque.getCompteClient(numCompteClient).getNip().matches(nip)){
                                cnx.envoyer("CONNECT OK");
                                cnx.setNumeroCompteClient(numCompteClient);
                                cnx.setNumeroCompteActuel(banque.getNumeroCompteParDefaut(numCompteClient));
                            }
                            cnx.envoyer("CONNECT NO");
                        }
                    } else {
                        System.out.println("Le compte existe pas dans le systeme");
                        cnx.envoyer("CONNECT NO");
                    }
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
                            cnx.setNumeroCompteClient(numCompteClient);
                            cnx.setNumeroCompteActuel(banque.getNumeroCompteParDefaut(numCompteClient));
                            cnx.envoyer("NOUVEAU OK " + t[0] + " cree");
                        }
                        else
                            cnx.envoyer("NOUVEAU NO "+t[0]+" existe deja");
                    }
                    break;
                case "EPARGNE":
                    if(cnx.getNumeroCompteClient()!=null && !serveurBanque.getBanque().verifSiDejaCompte(cnx.getNumeroCompteClient(),TypeCompte.valueOf("EPARGNE"))){
                        banque = serveurBanque.getBanque();
                        //creer on nouv compte bancaire
                        banque.getCompteClient(cnx.getNumeroCompteClient()).ajouter(new CompteEpargne(banque.getNumCompteBancaireValide(),TypeCompte.EPARGNE,5));
                        cnx.envoyer("EPARGNE OK");
                    } else {
                        cnx.envoyer("EPARGNE NO");
                    }
                    break;
                case "SELECT":
                    if(cnx.getNumeroCompteClient()!=null){
                        argument = evenement.getArgument();
                        banque = serveurBanque.getBanque();
                        if(banque.getCompteClient(cnx.getNumeroCompteClient()).verifTypeCompte(TypeCompte.EPARGNE)){
                            cnx.setNumeroCompteActuel(banque.getCompteClient(cnx.getNumeroCompteClient()).getNumCompteBancaire(TypeCompte.valueOf(argument)));
                            cnx.envoyer("SELECT OK");
                            break;
                        }
                    } 
                    cnx.envoyer("SELECT NO");
                    break;
                /******************* TRAITEMENT PAR DÉFAUT *******************/
                default: //Renvoyer le texte recu convertit en majuscules :
                    msg = (evenement.getType() + " " + evenement.getArgument()).toUpperCase();
                    cnx.envoyer(msg);
            }
        }
    }
}