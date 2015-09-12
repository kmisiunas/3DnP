# 3DnP

## ToDo

 - [X] TCP DAC communication
 - [X] JAVAFX GUI
 - [ ] Safe TCP connection handling and automatic re-connection
 - [X] Think of modular control structure. Needs to be flexible to allow dynamic swap of algorithms
 - [ ] Need current stabilisation/nulling algorithm: electrical drift + evaporation compensation
 - [ ] Hopping mode imaging
 - [ ] Processor with ability to kill Process 


### Logs

To view logs go to `log/3DnP-akka.log` and enter this command

    tail -F log/3DnP-akka.log
    tail -F log/3DnP-akka.log | less -S
    tail -F log/3DnP-akka.log | grep WARN  