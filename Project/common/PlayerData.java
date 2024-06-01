package Project.common;

import java.io.Serializable;

public class PlayerData implements Serializable{
    private int health;
    private int hits;
    private int misses;
    private int score ;
    private int currency;
    private boolean isTurn = false;
    private boolean isAway = false;

    // Should include the GameBoard in the future...

    public PlayerData(int health) {
      this.score = 0;
      this.health = health;
      this.hits = 0;
      this.misses = 0;
      this.currency = 0;
    }

    public PlayerData(int health, int hits, int misses, int score, int currency) {
      this.health = health;
      this.hits = hits;
      this.misses = misses;
      this.score = score;
      this.currency = currency;
    }

    public PlayerData(PlayerData player) {
      this.score = player.getScore();
      this.health = player.getHealth();
      this.hits = player.getHits();
      this.misses = player.getMisses();
      this.currency = player.getCurrency();
    }

    // --- Other ---

    public Integer[] getStats() { return new Integer[] {health, hits, misses, score, currency, isAway ? 1 : 0, isTurn ? 1 : 0}; }

    public synchronized void isAway(boolean isAway) { this.isAway = isAway; }

    public synchronized boolean isAway() { return isAway; }

    public synchronized void isTurn(boolean isTurn) { this.isTurn = isTurn; }

    public synchronized boolean isTurn() { return isTurn; }

    // --- Scores ---

    public synchronized int getScore() { return score; }

    public synchronized void setScore(int score) { this.score = score; }

    public synchronized void incrementScore() { score++; }

    public synchronized void incrementScore(int points) { score += points; }

    // --- Health ---

    public synchronized int getHealth() { return health; }

    public synchronized void setHealth(int health) { this.health = health; }

    public synchronized void incrementHealth() { health++; }

    public synchronized void incrementHealth(int health) { this.health += health; }

    public synchronized void decrementHealth() { health--; }

    public synchronized void decrementHealth(int damage) { health -= damage; }

    // --- Hits ---

    public synchronized int getHits() { return hits; }

    public synchronized void setHits(int hits) { this.hits = hits; }

    public synchronized void incrementHits() { hits++; }

    // --- Misses ---

    public synchronized int getMisses() { return misses; }

    public synchronized void setMisses(int misses) { this.misses = misses; }

    public synchronized void incrementMisses() { misses++; }

    // --- Currency ---

    public synchronized int getCurrency() { return currency; }

    public synchronized void incrementCurrency() { currency++; }
    
    public synchronized void incrementCurrency(int amount) { currency += amount; }

    public synchronized void decrementCurrency() { currency--; }

    public synchronized void decrementCurrency(int amount) { currency -= amount; }

    public synchronized void setCurrency(int currency) { this.currency = currency; }

    @Override
    public String toString() {
      return String.format("%d points, %d health, %d hits, %d misses, %d currency, isAway: %b, isTurn %b", score, health, hits, misses, currency, isAway, isTurn);
    }
}