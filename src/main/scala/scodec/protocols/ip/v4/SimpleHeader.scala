package scodec.protocols
package ip
package v4

import scalaz.std.anyVal.unitInstance
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import shapeless._

/** Simplified version of the IPv4 header format. */
case class SimpleHeader(
  dataLength: Int,
  id: Int,
  ttl: Int,
  protocol: Int,
  sourceIp: Address,
  destinationIp: Address
)

object SimpleHeader {

  implicit val codec: Codec[SimpleHeader] = {
    val componentCodec =
      ignore(8) :~>:
      ("ihl" | uint8) ::
      ("total_length" | uint16) ::
      ("id" | uint16) ::
      ignore(16) ::
      ("ttl" | uint8) ::
      ("proto" | uint8) ::
      ("checksum" | bits(16)) ::
      ("src_ip" | Codec[Address]) ::
      ("dest_ip" | Codec[Address])

    new Codec[SimpleHeader] {
      def encode(header: SimpleHeader) = {
        val totalLength = header.dataLength + 20
        for {
          encoded <- componentCodec.encode(5 :: totalLength :: header.id :: () :: header.ttl :: header.protocol :: BitVector.low(16) :: header.sourceIp :: header.destinationIp :: HNil)
          chksum = checksum(encoded)
        } yield encoded.patch(16 + 16 + 16, chksum)
      }

      def decode(bits: BitVector) = {
        componentCodec.decode(bits) map {
          case (rest, _ :: totalLength :: id :: _ :: ttl :: proto :: chksum :: srcIp :: dstIp :: HNil) =>
            rest -> SimpleHeader(totalLength - 20, id, ttl, proto, srcIp, dstIp)
        }
      }
    }
  }

}
