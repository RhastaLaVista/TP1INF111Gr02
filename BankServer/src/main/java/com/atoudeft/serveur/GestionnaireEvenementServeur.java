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
                    try {
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
                    } catch (NumberFormatException e) {
                        // Gérer le cas où l'argument n'est pas un nombre valide
                        cnx.envoyer("DEPOT NO!! Montant invalide. Veuillez entrer un nombre.");
                        break;
                    }

                case "RETRAIT": // Permet au client de débiter son compte.
                    argument = evenement.getArgument();
                    try {
                        // Conversion de l'argument en montant (double)
                        double montant;
                        try {
                            montant = Double.parseDouble(argument);
                        } catch (NumberFormatException e) {
                            cnx.envoyer("RETRAIT NO!! Montant invalide");
                            break;
                        }

                        // Vérification si le montant est positif
                        if (montant <= 0) {
                            cnx.envoyer("RETRAIT NO!! Montant invalide, doit être positif");
                            break;
                        }

                        // Récupération de la banque et des informations du client
                        Banque banqueRetrait = ((ServeurBanque) serveur).getBanque();
                        String numCompteClientRetrait = cnx.getNumeroCompteClient();

                        if (numCompteClientRetrait != null) {
                            CompteClient compteClient = banqueRetrait.getCompteClient(numCompteClientRetrait);

                            if (compteClient != null) {
                                // Récupération du compte bancaire actif
                                String numeroCompteActuel = cnx.getNumeroCompteActuel();
                                CompteBancaire compteActuel = null;

                                for (CompteBancaire compte : compteClient.getComptes()) {
                                    if (compte.getNumero().equals(numeroCompteActuel)) {
                                        compteActuel = compte;
                                        break;
                                    }
                                }

                                if (compteActuel != null) {
                                    // Application des frais pour les comptes épargne
                                    if (compteActuel instanceof CompteEpargne) {
                                        CompteEpargne compteEpargne = (CompteEpargne) compteActuel;

                                        // Vérifier si le compte est en dessous de la limite avant le retrait
                                        if (compteEpargne.getSolde() < 1000.0) {
                                            montant += 2.0; // Ajouter les frais
                                            cnx.envoyer("RETRAIT AVERTISSEMENT : Des frais de 2.0 $ ont été appliqués.");
                                        }
                                    }

                                    // Tentative de retrait
                                    if (compteActuel.debiter(montant)) {
                                        cnx.envoyer("RETRAIT OK Montant retiré : " + (montant - (compteActuel instanceof CompteEpargne ? 2.0 : 0)));
                                    } else {
                                        cnx.envoyer("RETRAIT NO Échec du retrait, fonds insuffisants");
                                    }
                                } else {
                                    cnx.envoyer("RETRAIT NO Compte sélectionné non trouvé");
                                }
                            } else {
                                cnx.envoyer("RETRAIT NO Compte-client non trouvé");
                            }
                        } else {
                            cnx.envoyer("RETRAIT NO Compte-client non trouvé");
                        }
                    } catch (Exception e) {
                        cnx.envoyer("RETRAIT NO Une erreur est survenue");
                    }
                    break;

                case "FACTURE": // Permet au client de payer une facture.
                    argument = evenement.getArgument(); // Récupérer les arguments de la commande.
                    String[] parts = argument.split(" "); // Diviser les arguments en parties.

                    // Vérification du format de la commande (au moins montant, numéro de facture, description)
                    if (parts.length < 3) {
                        cnx.envoyer("FACTURE NO!! Format incorrect");
                        break;
                    }

                    try {
                        // Extraction des parties
                        double montant = Double.parseDouble(parts[0]); // Le montant de la facture.
                        String numeroFacture = parts[1]; // Numéro de la facture.
                        String description = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length)); // Description.

                        // Vérifier que le montant est valide (positif)
                        if (montant <= 0) {
                            cnx.envoyer("FACTURE NO!! Montant invalide, doit être positif");
                            break;
                        }

                        // Accéder à la banque et aux informations du client
                        Banque banqueFacture = serveurBanque.getBanque();
                        String numCompteClientFacture = cnx.getNumeroCompteClient();

                        if (numCompteClientFacture != null) {
                            // Récupérer le compte client
                            CompteClient compteClient = banqueFacture.getCompteClient(numCompteClientFacture);

                            if (compteClient != null) {
                                // Identifier le compte actif du client
                                String numeroCompteActuelFacture = cnx.getNumeroCompteActuel();
                                CompteBancaire compteActuel = null;

                                for (CompteBancaire compte : compteClient.getComptes()) {
                                    if (compte.getNumero().equals(numeroCompteActuelFacture)) {
                                        compteActuel = compte;
                                        break;
                                    }
                                }

                                if (compteActuel != null) {
                                    // Si c'est un compte épargne, appliquer des frais
                                    if (compteActuel instanceof CompteEpargne) {
                                        montant += 2.0; // Ajouter des frais pour les comptes épargne
                                        cnx.envoyer("FACTURE AVERTISSEMENT : Des frais de 2.0 $ ont été appliqués.");
                                    }

                                    // Effectuer le paiement de la facture
                                    if (compteActuel.debiter(montant)) {
                                        cnx.envoyer("FACTURE OK Paiement effectué pour la facture " + numeroFacture + ": " + description);
                                    } else {
                                        cnx.envoyer("FACTURE NO Solde insuffisant ou erreur lors du paiement");
                                        break;
                                    }
                                } else {
                                    cnx.envoyer("FACTURE NO Compte bancaire non trouvé");
                                    break;
                                }
                            } else {
                                cnx.envoyer("FACTURE NO Compte client introuvable");
                                break;
                            }
                        } else {
                            cnx.envoyer("FACTURE NO Compte client non trouvé");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        // Gestion des erreurs de format pour le montant
                        cnx.envoyer("FACTURE NO!! Montant invalide");
                        break;
                    } catch (Exception e) {
                        cnx.envoyer("FACTURE NO Une erreur est survenue");
                        break;
                    }
                    break;


                case "TRANSFER": // Permet au client de transférer de l'argent à un autre compte
                    argument = evenement.getArgument(); // Récupère l'argument (format : montant numéro-compte)
                    String[] partsTransfer = argument.split(" "); // Divise l'argument en deux parties : montant et numéro-compte

                    // Vérifie que l'entrée contient bien un montant et un numéro de compte
                    if (partsTransfer.length < 2) {
                        cnx.envoyer("TRANSFER NO Format incorrect. Utilisez : TRANSFER montant numéro-compte");
                        break;
                    }

                    try {
                        // Extraction des informations depuis l'argument
                        double montantTransfer = Double.parseDouble(partsTransfer[0]); // Récupérer le montant
                        String numeroCompteDestinataire = partsTransfer[1]; // Récupérer le numéro de compte destinataire

                        // Vérifie que le montant est positif
                        if (montantTransfer <= 0) {
                            cnx.envoyer("TRANSFER NO Montant invalide. Il doit être strictement positif.");
                            break;
                        }

                        // Accès à la banque et au compte client actuel
                        banque = serveurBanque.getBanque(); // Récupère l'objet banque

                        int comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

                        if (cnx.getNumeroCompteClient() == null) {//Vérification que l'utilisateur est connectée
                            cnx.envoyer("TRANSFER NO UTILISATEUR NON CONNECTE");
                            break;

                        } if(cnx.getNumeroCompteClient() != null) {
                                // Identification du compte actif
                                String numeroCompteActuelTransfer = cnx.getNumeroCompteActuel(); // Compte actif
                                CompteBancaire compteActuelTransfer = null;

                                // Parcours des comptes pour trouver le compte actif
                                for (CompteBancaire compte : compteClientTransfer.getComptes()) {
                                    if (compte.getNumero().equals(numeroCompteActuelTransfer)) {
                                        compteActuelTransfer = compte;
                                        break;
                                    }
                                }


                                    // Si le compte actuel est un compte-épargne, appliquer les frais
                                    if (compteActuelTransfer instanceof CompteEpargne) {
                                        double frais = ((CompteEpargne) compteActuelTransfer).getPRELEVEMENT(); // Exemple de frais pour le compte-épargne
                                        montantTransfer += frais; // Ajouter les frais au montant
                                        cnx.envoyer("TRANSFER AVERTISSEMENT : Des frais de " + frais + " $ ont été appliqués pour le compte-épargne.");
                                    }

                                    // Vérifie que le compte destinataire existe
                                    for (CompteClient compte : banque.getComptes()) {
                                        for(CompteBancaire comptes : compte.getComptes()){
                                            if (comptes.getNumero().equals(numeroCompteDestinataire)) {
                                            }}}

                                        // Recherche du compte destinataire

                    } catch (NumberFormatException e) {
                        cnx.envoyer("TRANSFER NO Montant invalide. Veuillez entrer un nombre.");
                        break;
                    } catch (Exception e) {
                        cnx.envoyer("TRANSFER NO Une erreur inattendue s'est produite.");
                        break;
                    }
                    break;


                case "HIST":
                    banque = serveurBanque.getBanque();
                    int comptebancaireCourante = banque.getCompteClient(cnx.getNumeroCompteClient()).choixBancaire(cnx.getNumeroCompteActuel());

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
