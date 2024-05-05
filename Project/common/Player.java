package Project.common;

import java.io.Serializable;

public class Player implements Serializable{
    private String name;
    private int score ;
    private int health;
    private int hits;
    private int misses;
    private int currency;

    public Player(String name, int health) {
      this.score = 0;
      this.health = health;
      this.hits = 0;
      this.misses = 0;
      this.currency = 0;
    }

    public Player(String name, int score, int health, int hits, int misses, int currency) {
      this.name = name;
      this.score = score;
      this.health = health;
      this.hits = hits;
      this.misses = misses;
      this.currency = currency;
    }

    public Player(Player player) {
      this.name = player.getName();
      this.score = player.getScore();
      this.health = player.getHealth();
      this.hits = player.getHits();
      this.misses = player.getMisses();
      this.currency = player.getCurrency();
    }

    public String getName() { return name; }

    public int getScore() { return score; }

    public int getHealth() { return health; }

    public int getHits() { return hits; }

    public int getMisses() { return misses; }

    public int getCurrency() { return currency; }

    public void setName(String name) { this.name = name; }

    public void incrementScore() { score++; }

    public void incrementScore(int points) { score += points; }

    public void decrementHealth() { health--; }

    public void decrementHealth(int damage) { health -= damage; }

    public void incrementHits() { hits++; }

    public void incrementMisses() { misses++; }

    public void incrementCurrency() { currency++; }
    
    public void incrementCurrency(int amount) { currency += amount; }

    public void decrementCurrency() { currency--; }
    
    public void decrementCurrency(int amount) { currency -= amount; }

    public void setHealth(int health) { this.health = health; }

    public void setScore(int score) { this.score = score; }

    public void setHits(int hits) { this.hits = hits; }

    public void setMisses(int misses) { this.misses = misses; }

    public void setCurrency(int currency) { this.currency = currency; }

    @Override
    public String toString() {
      return String.format("%s: %d points, %d health, %d hits, %d misses, %d currency", name, score, health, hits, misses, currency);
    }
}