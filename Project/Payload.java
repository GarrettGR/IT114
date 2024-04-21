import java.io.Serializable;

public class Payload implements Serializable {
  private static final long serialVersionUID = 1L;
  private PayloadType payloadType;
  private String clientName;
  private String message;
  private int number;

  public PayloadType getPayloadType() { return payloadType; }

  public void setPayloadType(PayloadType payloadType) { this.payloadType = payloadType; }

  public String getClientName() { return clientName; }

  public void setClientName(String clientName) { this.clientName = clientName; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public int getNumber() { return number; }

  public void setNumber(int number) { this.number = number; }

  @Override
  public String toString() {
     return String.format("Type[%s], Number[%s], Message[%s]", getPayloadType().toString(), getNumber(), getMessage());
  }
}