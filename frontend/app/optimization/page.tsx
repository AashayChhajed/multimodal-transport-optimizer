import { redirect } from "next/navigation";
import { getShipments } from "@/lib/api";

export default async function OptimizationIndexPage() {
  const shipments = await getShipments();
  if (shipments.length === 0) {
    redirect("/create-shipment");
  }

  redirect(`/optimization/${shipments[shipments.length - 1].id}`);
}
