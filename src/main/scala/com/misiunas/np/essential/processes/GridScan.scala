package com.misiunas.np.essential.processes


import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess
import com.misiunas.np.essential.DeviceProcess._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scalax.io._
import breeze.stats._



/** # Scans the surface in rectangular grid ans saves to a file
  *
  * Perform serpentine walk
  * >------>--->
  * <----<-----|
  * \---->----->
  *
  * Created by kmisiunas on 2016-07-27.
  */
class GridScan (val target: Double) extends DeviceProcess {

  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)


  import collection.JavaConversions._
  val dx = ConfigFactory.load.getDouble("gridScan.dx")
  val dy  = ConfigFactory.load.getDouble("gridScan.dy")
  val safetyDz = ConfigFactory.load.getDouble("gridScan.safetyDz")
  val scanArea: List[Double] = ConfigFactory.load.getDoubleList("gridScan.scanArea").toList.map(_.toDouble)

  var grid: List[Vec] = Nil
  var totalSteps: Int = 0

  override def toString: String = "GridScan("+grid.length+"/"+totalSteps+")"

  var file: Output = null

  override def initialise(): Unit = {
    grid = {
      val steps = Array.ofDim[Vec]((scanArea(0)/dx).toInt , (scanArea(1)/dy).toInt)
      for (x <- steps.indices; y <- steps.head.indices){
        steps(x)(y) = Vec(x*dx, y*dy, 0.0)
      }
      for (x <- steps.indices){ //serpentine walk
        steps(x) = if(x% 2 == 0) steps(x) else steps(x).reverse
      }
      val linear: Array[Vec] = steps.flatten
      val start = probe.pos.copy(z = 0.0)
      linear.map( _ + start).toList
    }
    totalSteps = grid.length
    file = Resource.fromFile("GridScan on "+DateTime.now().toString()+".csv")

  }


  private var status: String = "init"

  override def step(): StepResponse = status match {
    case "init" =>
      status = "read"
      InjectProcess( Approach.manual(target, true, 10) )
    case "read" =>
      status = "next"
      val pos = probe.pos
      val line: String = DateTime.now().toString() + ", " +
                          pos.x +", "+pos.y+", "+pos.z+", " +
                          mean( amplifier.trackList(10) ) + ", " +
                          stddev( amplifier.trackList(10) ) +", " +
                          amplifier.trackBaselineCurrent
      file.write(line)(Codec.UTF8)
      grid = grid.tail
      DeviceProcess.Continue
    case "next" if grid.isEmpty =>
      Finished
    case "next" =>
      probe.moveBy( Vec(0,0,-safetyDz) )
      probe.move( probe.pos.copy(x = grid.head.x, y = grid.head.y) )
      InjectProcess( Approach.manual(target, true, 10) )

    case _ => Panic("Command not found")
  }

  override def finalise(): ProcessResults = {
    probe.moveBy( Vec(0,0,-safetyDz) )
    Success
  }

}

object GridScan {
  def apply(target: Double): GridScan = new GridScan(target)
}

