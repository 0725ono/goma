import { Slot } from "expo-router";

export default function PublicLayout() {
  // ヘッダーなどを表示せずそのまま描画
  return <Slot />;
}
