#include <SoftwareSerial.h>

#define BPM_LED_PIN 13
#define BPM_LONG_LED_PIN 5
#define POWER_BUTTON_PIN 9
#define POWER_LED_PIN 4

//Portas do BT 
SoftwareSerial btSerial(10, 11); // RX, TX


// Usado para fazer a animação do LED BPM_LONG_LED_PIN
int gBPMLongTaxa = 0;
#define BPM_LONG_MAX 255


// Variáveis voláteis, utilizado na rotina de serviço de interrupção!
volatile int BPM; // contém o valor dos batimentos retornados no pino 0. atualizado a cada 2mS


volatile boolean QS = false; // Verdadeiro quando o android encontra um batimento

boolean gLigado = false;

//colocar invertido os cabos;

void setup()
{
  pinMode(BPM_LED_PIN, OUTPUT); 
  pinMode(BPM_LONG_LED_PIN, OUTPUT);    
  pinMode(POWER_LED_PIN, OUTPUT);
  pinMode(POWER_BUTTON_PIN, INPUT);  

  btSerial.begin(9600);
  Serial.begin(115200);
    
  interruptSetup(); // define a leitura do sinal do sensor de pulso a cada 2mS   
}


void enabled_leds(boolean enabled)
{
  if (!enabled) 
  {
    digitalWrite(BPM_LED_PIN, LOW);
    digitalWrite(BPM_LONG_LED_PIN, LOW);
    digitalWrite(POWER_LED_PIN, LOW);
  }
  else
    digitalWrite(POWER_LED_PIN, HIGH);
}


void dalay_custom(int d) { delay(d); }


void ligar_sensor(void)
{   
  if(btSerial.available()) 
  {    
    char letra = btSerial.read();
    if (letra == '1')
    {
      Serial.println("--- LIGADO ---");
      gLigado = true;
      enabled_leds(true);
    }
    else if (letra == '0')
    {
      Serial.println("--- DESLIGADO ---");
      gLigado = false;
      enabled_leds(false);
    }      
  }
  else if (digitalRead(POWER_BUTTON_PIN) == HIGH) 
  {
    if (gLigado = !gLigado)
    {      
      Serial.println("--- LIGADO ---");
      enabled_leds(true);
    }
    else 
    {      
      Serial.println("--- DESLIGADO ---");
      enabled_leds(false);
    }
    dalay_custom(200);
  }  
}

void loop()
{
  if (gLigado) 
  {     
    if (QS == true)
    {     
      /* Se existe um batimento
       * Então o BPM IBI foram determinados */ 
                           
      digitalWrite(BPM_LED_PIN,HIGH);
      gBPMLongTaxa = BPM_LONG_MAX;                                  
      
      Serial.print("BPM: ");
      Serial.println(BPM);
      btSerial.println(BPM);
       
      
      QS = false;  // Reseta para proxima vez    
    } 
    else               
      digitalWrite(BPM_LED_PIN,LOW);
    
         
    ledFadeToBeat(); //Efeito no Led 
    dalay_custom(20);
  }
  
  ligar_sensor();    
}

/*Função utilizada para animação do LED Batimento */
void ledFadeToBeat()
{
    gBPMLongTaxa -= 15;
    gBPMLongTaxa = constrain(gBPMLongTaxa, 0, BPM_LONG_MAX); //-> [a N b = N], [N a b = a], [a b N = N]   
    analogWrite(BPM_LONG_LED_PIN, gBPMLongTaxa);
}
