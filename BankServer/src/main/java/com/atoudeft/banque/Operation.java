package com.atoudeft.banque;

import java.io.Serializable;

import java.util.Date;


public abstract class Operation implements Serializable {
    //date d'operation
    private Date date = new Date();

    //type d'operation
    TypeOperation type;
    //constructor data
    double montant = 0;

    //methods:
    public Operation(double montant, TypeOperation type)
    {
        this.montant = montant;
        this.type = type;

    }//retrait & depot constructor

    public Operation(double montant, TypeOperation type, String numeroDestinataire)
    {
        this.montant = montant;
        this.type = type;

    }//transferconstructor

    public Operation(double montant, TypeOperation type, String numFacture, String descFacture)
    {
        this.montant = montant;
        this.type = type;

    }//factureConstructor

    public Date getDate() {
        return date;
    }

    public TypeOperation getType() {
        return type;
    }

    public double getMontant() {
        return montant;
    }
}


