package com.misiunas.np.essential.processes

import breeze.stats._
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess
import com.misiunas.np.essential.DeviceProcess._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scalax.io._
import scalax.io.{Seekable,Codec}
import breeze.stats._

/**
  * Created by kmisiunas on 2016-08-14.
  */
class SampleDeposition (dZ: Double) extends DeviceProcess {

  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)


  import collection.JavaConversions._

  val target = 0.85
  val dx = 5.0
  val dy  = 5.0
  val safetyDz = 10.0
  val scanArea: List[Double] = List(90,90)

  var grid: List[Vec] = Nil
  var totalSteps: Int = 0

  var voltage = 100.0
  val dV = 50.0
  val minV = 100.0
  val maxV = 1000.0

  var t = 0.005
  val dt = 0.005
  val maxt = 0.05
  val mint = 0.005

  override def toString: String = "SampleDeposition("+(totalSteps-grid.length+1)+"/"+totalSteps+")"

  var file: Seekable = null

  override def onStart(): Unit = {
    grid = {
      val steps = Array.ofDim[Vec]((scanArea(0)/dx).toInt , (scanArea(1)/dy).toInt)
      for (x <- steps.indices; y <- steps.head.indices){
        steps(x)(y) = Vec(x*dx, y*dy, 0.0)
      }
      for (x <- steps.indices){ //serpentine walk
        steps(x) = if(x% 2 == 0) steps(x) else steps(x).reverse
      }
      val linear: Array[Vec] = steps.flatten
      val start = Vec(10, 10, 0)
      linear.map( _ + start).toList
    }
    totalSteps = grid.length
    //file = Resource.fromFile("GridScan on "+DateTime.now().toString()+".csv")
    file = Resource.fromFile("SampleDeposition.csv")
  }


  private var status: String = "init"

  override def step(): StepResponse = status match {
    case "init" =>
      status = "write"
      probe.move( probe.pos.copy(x = grid.head.x, y = grid.head.y) )
      InjectProcess( Approach.manualOld(target, true, 10) )
    case "write" =>
      status = "next"
      val pos = probe.pos
      val line: String = DateTime.now().toString() + ", " +
        pos.x +", "+pos.y+", "+pos.z+", " +
        mean( amplifier.trackList(10) ) + ", " +
        stddev( amplifier.trackList(10) ) +", " +
        amplifier.trackBaselineCurrent + ", " +
        dZ + ", " +
        voltage + ", "+
        t
      try{
        file.append(line+"\n")
        log.warn(line)
      } catch {
        case _ => log.warn(line)
      }
      grid = grid.tail
      // deposit
      probe.moveBy( Vec(0,0,-dZ) )
      amplifier.pulse(t)
      DeviceProcess.Continue
    case "next" if grid.isEmpty =>
      probe.moveBy( Vec(0,0,-safetyDz) )
      Finished
    case "next" =>
      status = "write"
      probe.moveBy( Vec(0,0,-safetyDz) )
      t = if (t+dt > maxt) {
        voltage = math.min(voltage + dV, maxV)
        mint
      } else {
        t + dt
      }
      amplifier.setPulseVoltage(voltage)
      probe.move( probe.pos.copy(x = grid.head.x, y = grid.head.y) )
      amplifier.await(10)
      InjectProcess( Approach.manualOld(target, true, 1) )

    case _ => Panic("Command not found")
  }

  override def onStop(): ProcessResults = {
    probe.moveBy( Vec(0,0,-safetyDz) )
    Success
  }

}

object SampleDeposition {
  def apply(dZ: Double): SampleDeposition = new SampleDeposition(dZ)
}

