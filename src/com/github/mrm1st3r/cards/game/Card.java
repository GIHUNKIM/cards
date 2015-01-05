package com.github.mrm1st3r.cards.game;

import com.github.mrm1st3r.cards.R;

import android.media.Image;

/**
 * Spielkarte
 * 
 * @author Sergius Maier
 * @version 0.8
 */
public class Card {
	
	/**
	 * Name der Karte (2, 3, 4,..., Bube, Dame, K�nig, Ass)
	 */
	String name;	
	/**
	 * Farbe als Zeichenkette
	 */
	String color;
	/**
	 * Farbe als Zahl.<br>
	 * 0 = Herz, 1 = Karo, 2 = Pik, 3 = Kreuz
	 */
	int colour;
	/**
	 * Wert des Spielkarte
	 */
	int value;
	/**
	 * URI des Kartenbilds
	 */
	String image;
	
	/**
	 * Konstruktor der Klasse "Card"
	 * 
	 * @param n	Kartenbezeichnung
	 * @param c	Kartenfarbe (0 = Herz, 1 = Karo, 2 = Pik, 3 = Kreuz)
	 * @param v	Kartenwert
	 */
	public Card(String n, int c, int v){
		setName(n);
		setValue(v);
		setColour(c);
		String temp = "";
		switch(c){
		case 0:
			temp = "hearts";	// Herz
			break;
		case 1:
			temp = "diamonds";	// Karo
			break;
		case 2:
			temp = "spades";	// Pik
			break;
		case 3:
			temp = "clubs";		// Kreuz
			break;
		}
		setColor(temp);
		setImage("card_" + color + "_" + name);
	}
	
	/**
	 * Getter f�r {@link #name}
	 * 
	 * @return {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setter f�r {@link #name}
	 * 
	 * @param name um {@link #name} zu definieren
	 */
	private void setName(String name) {
		this.name = name;
	}

	/**
	 * Getter f�r {@link #color}
	 * 
	 * @return {@link #color}
	 */
	public String getColor() {
		return color;
	}

	/**
	 * Setter f�r {@link #color}
	 * 
	 * @param color um {@link #color} zu definieren
	 */
	private void setColor(String color) {
		this.color = color;
	}

	/**
	 * Getter f�r {@link #colour}
	 * 
	 * @return {@link #colour}
	 */
	public int getColour() {
		return colour;
	}

	/**
	 * Setter f�r {@link #colour}
	 * 
	 * @param colour um {@link #colour} zu definieren
	 */
	private void setColour(int colour) {
		this.colour = colour;
	}

	/**
	 * Getter f�r {@link #value}
	 * 
	 * @return {@link #value}
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Setter f�r {@link #value}
	 * 
	 * @param value um {@link #value} zu definieren
	 */
	private void setValue(int value) {
		this.value = value;
	}
	
	/**
	 * Getter f�r {@link #image}
	 * 
	 * @return {@link #image}
	 */
	public String getImage() {
		return image;
	}

	/**
	 * Setter f�r {@link #image}
	 * 
	 * @param image um {@link #image} zu definieren
	 */
	public void setImage(String image) {
		this.image = image;
	}

	@Override
	public String toString(){
		return color + " " + name;
	}
}