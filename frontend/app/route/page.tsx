import { redirect } from "next/navigation";
import { getShipments } from "@/lib/api";

export default async function RouteIndexPage() {
  const shipments = await getShipments();
  if (shipments.length === 0) {
    redirect("/create-shipment");
  }

  redirect(`/route/${shipments[shipments.length - 1].id}`);
}
