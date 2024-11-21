package com.atoudeft.serveur;

import com.atoudeft.banque.Banque;
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
                            cnx.envoyer("NOUVEAU NO "+t[0]+" existe");
                    }
                    break;
                case "DEPOT": // permettre au client de créditer son compte.
                    argument = evenement.getArgument();
                    try{
                        cnx.envoyer("DEPOT: " + argument);
                        // conversion du montant en float
                        Float montant = Float.valueOf(argument);
                        // Vérifier si le montant est valide (positif)
                        if ( montant <= 0) {
                            System.out.println(montant);
                            cnx.envoyer("DEPOT NO Montant invalide");
                        } else {
                            // Trouver le compte du client à partir du numéro de compte-client
                            Banque banqueDepot = ((ServeurBanque) serveur).getBanque();
                            String numCompteClientDepot = cnx.getNumeroCompteClient();
                            if (numCompteClientDepot != null) {
                                // Effectuer le dépôt sur le compte
                                if (banqueDepot.deposer(montant.doubleValue(), numCompteClientDepot)) {
                                    cnx.envoyer("DEPOT OK Montant déposé : " + montant);
                                } else {
                                    cnx.envoyer("DEPOT NO Échec du dépôt");
                                }
                            } else {
                                cnx.envoyer("DEPOT NO Compte-client non trouvé");
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Si l'argument ne peut pas être converti en nombre
                        cnx.envoyer("DEPOT NO Montant invalide");
                    }
                    break;
                case "RETRAIT":
                    // Récupérer l'argument, qui est le montant à retirer
                    argument = evenement.getArgument();
                    try {
                        // Validation de l'argument directement dans le code
                        Float montant = null;
                        try {
                            montant = Float.valueOf(argument);
                        } catch (NumberFormatException e) {
                            cnx.envoyer("RETRAIT NO Montant invalide");
                            break; // Si le montant n'est pas valide, sortir du case
                        }

                        // Vérifier que le montant est valide (positif)
                        if (montant <= 0) {
                            cnx.envoyer("RETRAIT NO Montant invalide, doit être positif");
                            break;
                        }

                        // Trouver le compte client à partir du numéro de compte-client
                        Banque banqueRetrait = ((ServeurBanque) serveur).getBanque();
                        String numCompteClientRetrait = cnx.getNumeroCompteClient();

                        if (numCompteClientRetrait != null) {
                            // Chercher le compte client à partir de la liste des comptes
                            CompteClient compteClient = banqueRetrait.getCompteClient(numCompteClientRetrait);
                            if (compteClient != null) {
                                String numeroCompteActuel = cnx.getNumeroCompteActuel();
                                CompteBancaire compteActuel = null;

                                // Parcours des comptes pour trouver celui qui correspond au numéro actuel
                                for (CompteBancaire compte : compteClient.getComptes()) {
                                    if (compte.getNumero().equals(numeroCompteActuel)) {
                                        compteActuel = compte;
                                        break;
                                    }
                                }

                                if (compteActuel != null) {
                                    // Si le compte est un compte épargne, appliquer des frais
                                    if (compteActuel.getType() == TypeCompte.EPARGNE) {
                                        float frais = 5.0F; // Exemple de frais fixes
                                        montant += frais; // Ajouter les frais au montant
                                        cnx.envoyer("RETRAIT AVERTISSEMENT Des frais de " + frais + " seront appliqués pour le retrait");
                                    }

                                    // Effectuer le retrait
                                    if (banqueRetrait.retirer(montant.doubleValue(), numCompteClientRetrait)) {
                                        cnx.envoyer("RETRAIT OK Montant retiré : " + montant);
                                    } else {
                                        cnx.envoyer("RETRAIT NO Échec du retrait, fonds insuffisants ou autre erreur");
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
                case "FACTURE":
                    argument = evenement.getArgument(); // Récupère l'argument (montant NUMFACT Description)
                    String[] parts = argument.split(" "); // Divise l'argument en parties (montant, NUMFACT, Description)

                    // Vérifie si l'argument contient au moins 3 éléments
                    if (parts.length < 3) {
                        cnx.envoyer("FACTURE NO Format incorrect");
                        break;
                    }

                    // Récupère les valeurs
                    float montant = Float.parseFloat(parts[0]); // Montant de la facture
                    String numeroFacture = parts[1]; // Numéro de la facture
                    String description = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length)); // Description de la facture

                    // Vérifie si le montant est valide
                    if (montant <= 0) {
                        cnx.envoyer("FACTURE NO Montant invalide");
                        break;
                    }

                    // Utiliser la variable serveurBanque déjà déclarée pour accéder à la banque
                    Banque banqueFacture = serveurBanque.getBanque(); // Obtenir la banque

                    // Vérifie le compte du client
                    String numCompteClientFacture = cnx.getNumeroCompteClient();
                    if (numCompteClientFacture != null) {
                        String numeroCompteActuelFacture = cnx.getNumeroCompteActuel();
                        CompteClient compteClient = banqueFacture.getCompteClient(numCompteClientFacture);

                        // Parcourt les comptes pour trouver celui qui correspond au compte actuel
                        CompteBancaire compteActuel = null;
                        for (CompteBancaire compte : compteClient.getComptes()) {
                            if (compte.getNumero().equals(numeroCompteActuelFacture)) {
                                compteActuel = compte;
                                break;
                            }
                        }

                        // Si c'est un compte épargne, appliquer des frais
                        if (compteActuel != null && compteActuel.getType() == TypeCompte.EPARGNE) {
                            montant += 5.0f; // Ajouter des frais fixes pour le compte épargne
                            cnx.envoyer("FACTURE AVERTISSEMENT Des frais de 5.0 ont été appliqués.");
                        }

                        // Effectuer le paiement de la facture (retrait du montant)
                        if (banqueFacture.retirer(montant, numCompteClientFacture)) {
                            cnx.envoyer("FACTURE OK Paiement effectué pour la facture " + numeroFacture + ": " + description);
                        } else {
                            cnx.envoyer("FACTURE NO Solde insuffisant");
                        }
                    } else {
                        cnx.envoyer("FACTURE NO Compte non trouvé");
                    }
                    break;
                case "TRANSFER":
                    argument = evenement.getArgument(); // Récupère l'argument (montant numéro-compte)
                    String[] partsTransfer = argument.split(" "); // Divise l'argument en parties (montant, numero-compte)

                    // Vérifie si l'argument contient au moins 2 éléments (montant et numero-compte)
                    if (partsTransfer.length < 2) {
                        cnx.envoyer("TRANSFER NO Format incorrect");
                        break;
                    }

                    // Récupère les valeurs
                    float montantTransfer = Float.parseFloat(partsTransfer[0]); // Montant à transférer
                    String numeroCompteDestinataire = partsTransfer[1]; // Numéro du compte destinataire

                    // Vérifie si le montant est valide
                    if (montantTransfer <= 0) {
                        cnx.envoyer("TRANSFER NO Montant invalide");
                        break;
                    }

                    // Utilise serveurBanque pour accéder à la banque
                    Banque banqueTransfer = serveurBanque.getBanque(); // Obtenir la banque

                    // Vérifie le compte du client
                    String numCompteClientTransfer = cnx.getNumeroCompteClient();
                    if (numCompteClientTransfer != null) {
                        // Récupère le compte client
                        CompteClient compteClientTransfer = banqueTransfer.getCompteClient(numCompteClientTransfer);

                        // Vérifie si le compte du client existe
                        if (compteClientTransfer != null) {
                            // Vérifie que le compte actuel est bien un compte dans la liste
                            String numeroCompteActuelTransfer = cnx.getNumeroCompteActuel();
                            CompteBancaire compteActuelTransfer = null;
                            for (CompteBancaire compte : compteClientTransfer.getComptes()) {
                                if (compte.getNumero().equals(numeroCompteActuelTransfer)) {
                                    compteActuelTransfer = compte;
                                    break;
                                }
                            }

                            // Vérifie si le compte existe
                            if (compteActuelTransfer != null) {
                                // Vérifie si c'est un compte épargne et applique des frais
                                if (compteActuelTransfer.getType() == TypeCompte.EPARGNE) {
                                    montantTransfer += 5.0f; // Ajouter des frais pour le compte épargne
                                    cnx.envoyer("TRANSFER AVERTISSEMENT Des frais de 5.0 ont été appliqués.");
                                }

                                // Effectuer le transfert
                                // Vérifie si le montant est suffisant
                                if (banqueTransfer.retirer(montantTransfer, numeroCompteActuelTransfer)) {
                                    // Vérifie que le compte destinataire existe
                                    CompteClient destinataireClient = banqueTransfer.getCompteClient(numeroCompteDestinataire);
                                    if (destinataireClient != null) {
                                        boolean compteDestinataireTrouve = false;
                                        for (CompteBancaire compte : destinataireClient.getComptes()) {
                                            if (compte.getNumero().equals(numeroCompteDestinataire)) {
                                                compteDestinataireTrouve = true;
                                                break;
                                            }
                                        }

                                        if (compteDestinataireTrouve) {
                                            // Effectue le dépôt sur le compte destinataire
                                            banqueTransfer.deposer(montantTransfer, numeroCompteDestinataire);
                                            cnx.envoyer("TRANSFER OK Montant transféré vers le compte " + numeroCompteDestinataire);
                                        } else {
                                            cnx.envoyer("TRANSFER NO Compte destinataire non trouvé");
                                        }
                                    } else {
                                        cnx.envoyer("TRANSFER NO Compte destinataire non trouvé");
                                    }
                                } else {
                                    cnx.envoyer("TRANSFER NO Solde insuffisant");
                                }
                            } else {
                                cnx.envoyer("TRANSFER NO Compte actuel non trouvé");
                            }
                        } else {
                            cnx.envoyer("TRANSFER NO Compte client non trouvé");
                        }
                    } else {
                        cnx.envoyer("TRANSFER NO Compte-client non trouvé");
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