# 3DnP

## ToDo Now

 - [ ] GUI control for PID controller
 - [ ] Logging + GUI activation 

 
## ToDo Longterm
 
 - [X] TCP DAC communication
 - [X] JAVAFX GUI
 - [ ] Safe TCP connection handling and automatic re-connection
 - [X] Think of modular control structure. Needs to be flexible to allow dynamic swap of algorithms
 - [ ] Need current stabilisation/nulling algorithm: electrical drift + evaporation compensation
 - [ ] Hopping mode imaging
 - [X] Processor with ability to kill Process 
 
## Guidelines

 - Need to build a modular system, where big components rely on simpler ones
 - Blockade mechanism for tasks (eg. no XY motion while doing baseline measurement)
 - Robust error handling when there in the tasks
 
### GUI
 
 - Window width up to 700px


### Logs

To view logs go to `log/3DnP-akka.log` and enter this command

    tail -F log/3DnP-akka.log
    tail -F log/3DnP-akka.log | less -S
    tail -F log/3DnP-akka.log | grep WARN  