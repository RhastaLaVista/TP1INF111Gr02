package com.atoudeft.banque;

import java.util.Calendar;

public abstract class Operation {
    //date d'operation
    
    //type d'operation
    TypeOperation type;

    //methods:
    public Operation(double montant){}//retrait & depot constructor

    public Operation(double montant,String numeroDestinataire){}//transferconstructor

    public Operation(double montant,String numFacture,String descFacture){}//factureConstructor

    //getDateofcreation

    //



}


