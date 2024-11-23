package com.atoudeft.serveur;

import com.atoudeft.banque.Banque;
import com.atoudeft.banque.CompteEpargne;
import com.atoudeft.banque.Operation;
import com.atoudeft.banque.CompteClient;
import com.atoudeft.banque.TypeCompte;
import com.atoudeft.banque.serveur.ConnexionBanque;
import com.atoudeft.banque.serveur.ServeurBanque;
import com.atoudeft.commun.evenement.Evenement;
import com.atoudeft.commun.evenement.GestionnaireEvenement;
import com.atoudeft.commun.net.Connexion;
import com.atoudeft.banque.CompteBancaire;

import java.util.Arrays;

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
        ServeurBanque serveurBanque = (ServeurBanque) serveur;
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
                    if (banque.compteExiste(numCompteClient)) {
                        //vérification de si ce compte est déja connectée dans une autre instance de BankClient
                        for (Connexion connected : serveur.connectes) {
                            if (((ConnexionBanque) connected).getNumeroCompteClient() != null) {
                                if ((connected instanceof ConnexionBanque && ((ConnexionBanque) connected).getNumeroCompteClient().equals(numCompteClient))) {
                                    System.out.println("deja connecte ailleurs");
                                    cnx.envoyer("CONNECT NO");
                                    dejaConnecte = true;
                                }
                            }
                        }
                        if (!dejaConnecte) {
                            //vérification des credentiels envoyés.
                            if (banque.getCompteClient(numCompteClient).getNip().matches(nip)) {
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
                    if (cnx.getNumeroCompteClient() != null) {
                        cnx.envoyer("NOUVEAU NO deja connecte");
                        break;
                    }
                    argument = evenement.getArgument();
                    t = argument.split(":");
                    if (t.length < 2) {
                        cnx.envoyer("NOUVEAU NO");
                    } else {
                        numCompteClient = t[0];
                        nip = t[1];
                        banque = serveurBanque.getBanque();
                        if (banque.ajouter(numCompteClient, nip)) {
                            cnx.setNumeroCompteClient(numCompteClient); // on se connect quand on cree un compte ?
                            cnx.setNumeroCompteActuel(banque.getNumeroCompteParDefaut(numCompteClient));
                            cnx.envoyer("NOUVEAU OK " + t[0] + " cree");
                        } else
                            cnx.envoyer("NOUVEAU NO " + t[0] + " existe");
                    }
                    break;
                case "EPARGNE":
                    if (cnx.getNumeroCompteClient() != null && !serveurBanque.getBanque().verifSiDejaCompte(cnx.getNumeroCompteClient(), TypeCompte.valueOf("EPARGNE"))) {
                        banque = serveurBanque.getBanque();
                        //creer on nouv compte ?
                        banque.getCompteClient(cnx.getNumeroCompteClient()).ajouter(new CompteEpargne(banque.getNumCompteBancaireValide(), TypeCompte.EPARGNE, 5));
                        //on send un msg ou non ?
                        cnx.envoyer("EPARGNE OK");
                    } else {
                        cnx.envoyer("EPARGNE NO");
                    }
                    break;
                case "SELECT":
                    if (cnx.getNumeroCompteClient() != null) {
                        argument = evenement.getArgument();
                        banque = serveurBanque.getBanque();
                        cnx.setNumeroCompteActuel(banque.getCompteClient(cnx.getNumeroCompteClient()).getNumCompteBancaire(TypeCompte.valueOf(argument)));
                        cnx.envoyer("SELECT OK");
                    } else {
                        cnx.envoyer("SELECT NO");
                    }
                    break;
                case "DEPOT": // Permettre au client de créditer son compte
                    argument = evenement.getArgument();

                        cnx.envoyer("DEPOT: " + argument);

                        // Convertir l'argument en nombre
                        double montant = Double.parseDouble(argument);

                        // Vérifier que le montant est positif
                        if (montant <= 0) {
                            cnx.envoyer("DEPOT NO! Montant invalide");
                            break;
                        }
                        // Récupérer le compte du client
                        banque = ((ServeurBanque) serveur).getBanque();
                        String numeroCompteClient = cnx.getNumeroCompteClient(); // Le client connecté
                        int comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                        if (numeroCompteClient != null) {
                            // Effectuer le dépôt
                            boolean depotReussi = banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).crediter(montant);
                            if (depotReussi) {
                                cnx.envoyer("DEPOT OK! Montant déposé : " + montant);
                                break;
                            } else {
                                cnx.envoyer("DEPOT NO!! Impossible de créditer ce compte.");
                                break;
                            }
                        } else {
                            cnx.envoyer("DEPOT NO!! Compte-client non trouvé.");
                            break;
                        }


                case "RETRAIT": // Permet au client de débiter son compte.
                    argument = evenement.getArgument();
                    banque = ((ServeurBanque) serveur).getBanque();
                    double montantRetraite = Double.parseDouble(argument);
                     comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                        // Vérification si le montant est positif
                        if (montantRetraite <= 0) {
                            cnx.envoyer("RETRAIT NO!! Montant invalide, doit être positif");
                            break;
                        }
                        // Récupération de la banque et des informations du client
                        banque = ((ServeurBanque) serveur).getBanque();
                        String numCompteClientRetrait = cnx.getNumeroCompteClient();

                        if (numCompteClientRetrait == null) {//s'arrange qu'on se connecte avant de faire qui que ce soit.
                            CompteClient compteClient = banque.getCompteClient(numCompteClientRetrait);
                        }

                                // Récupération du compte bancaire actif
                                String numeroCompteActuel = cnx.getNumeroCompteActuel();

                                    // Tentative de retrait
                                    if (banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).debiter(montantRetraite)) {
                                        cnx.envoyer("RETRAIT OK Montant retiré : " + (montantRetraite - (banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante) instanceof CompteEpargne ? 2.0 : 0)));
                                    } else {
                                        cnx.envoyer("RETRAIT NO Échec du retrait, fonds insuffisants");
                                    }break;

                case "FACTURE": // Permet au client de payer une facture.
                    argument = evenement.getArgument(); // Récupérer les arguments de la commande.
                    t = argument.split(" "); // Diviser les arguments en parties.
                    banque = ((ServeurBanque) serveur).getBanque();
                    comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                    // Vérification du format de la commande (au moins montant, numéro de facture, description)
                    if (t.length < 3) {
                        cnx.envoyer("FACTURE NO!! Format incorrect");
                        break;
                    }
                        // Extraction des parties
                        double montantFacture = Double.parseDouble(t[0]); // Le montant de la facture.
                        String numeroFacture = t[1]; // Numéro de la facture.
                        String description = String.join(" ", Arrays.copyOfRange(t, 2, t.length)); // Description.

                        // Vérifier que le montant est valide (positif)
                        if (montantFacture <= 0) {
                            cnx.envoyer("FACTURE NO!! Montant invalide, doit être positif");
                            break;
                        }

                        // Accéder à la banque et aux informations du client
                        if (cnx.getNumeroCompteClient() == null) {//assurance que l'utilisateur est connectée
                            cnx.envoyer("FACTURE NO CONNECTEZ-VOUS EN PREMIER");
                            break;
                        }
                        // Essaie paiement vers facture
                        if(banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).payerFacture(numeroFacture,montantFacture,description)){
                            cnx.envoyer("FACTURE OK" + montantFacture + numeroFacture + description);
                        }else{
                            cnx.envoyer("FACTURE NO VOUS PAS ASSEZ D'ARGENT");
                        }break;


                case "TRANSFER": // Permet au client de transférer de l'argent à un autre compte
                    argument = evenement.getArgument(); // Récupère l'argument (format : montant numéro-compte)
                    t = argument.split(" "); // Divise l'argument en deux parties : montant et numéro-compte

                    // Vérifie que l'entrée contient bien un montant et un numéro de compte
                    if (t.length < 2) {
                        cnx.envoyer("TRANSFER NO Format incorrect. Utilisez : TRANSFER montant numéro-compte");
                        break;
                    }

                        // Extraction des informations depuis l'argument
                        double montantTransfer = Double.parseDouble(t[0]); // Récupérer le montant
                        String numeroCompteBanqueDestinataire = t[1]; // Récupérer le numéro de compte destinataire

                        // Vérifie que le montant est positif
                        if (montantTransfer <= 0) {
                            cnx.envoyer("TRANSFER NO Montant invalide. Il doit être strictement positif.");
                            break;
                        }

                        // Accès à la banque et au compte client actuel
                        banque = serveurBanque.getBanque(); // Récupère l'objet banque

                        comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                        if (cnx.getNumeroCompteClient() == null) {//Vérification que l'utilisateur est connectée
                            cnx.envoyer("TRANSFER NO UTILISATEUR NON CONNECTE");
                            break;

                        } if(cnx.getNumeroCompteClient() != null) {
                                // Identification du compte actif
                                String numeroCompteActuelTransfer = cnx.getNumeroCompteActuel(); // Compte actif
                                CompteBancaire compteActuelTransfer = null;
                                

                                    // Vérifie que le compte destinataire existe et transfers
                                    for (CompteClient compte : banque.getComptes()) {
                                        for(CompteBancaire comptes : compte.getComptes()){
                                            if (comptes.getNumero().equals(numeroCompteBanqueDestinataire)) {
                                                if(banque.getCompteClient(cnx.getNumeroCompteClient()).getComptes().get(comptebancaireCourante).transferer(montantTransfer,numeroCompteBanqueDestinataire))
                                                    cnx.envoyer("TRANSFER OK "+ montantTransfer+"ENVOYE");
                                                    break;
                                                }
                                            }
                                        }
                                    }break;

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
