package Project.common;

import java.io.Serializable;

public class PlayerData implements Serializable{
    private int score ;
    private int health;
    private int hits;
    private int misses;
    private int currency;

    // Should include the GameBoard in the future...

    public PlayerData(int health) {
      this.score = 0;
      this.health = health;
      this.hits = 0;
      this.misses = 0;
      this.currency = 0;
    }

    public PlayerData(int score, int health, int hits, int misses, int currency) {
      this.score = score;
      this.health = health;
      this.hits = hits;
      this.misses = misses;
      this.currency = currency;
    }

    public PlayerData(PlayerData player) {
      this.score = player.getScore();
      this.health = player.getHealth();
      this.hits = player.getHits();
      this.misses = player.getMisses();
      this.currency = player.getCurrency();
    }

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
      return String.format("%d points, %d health, %d hits, %d misses, %d currency", score, health, hits, misses, currency);
    }
}