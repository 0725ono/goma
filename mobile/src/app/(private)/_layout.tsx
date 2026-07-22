import { Tabs } from "expo-router";

import { AppHeader } from "@/components/AppHeader";
import { SignOutButton } from "@/components/SignOutButton";

export default function PrivateLayout() {
  return (
    <Tabs
      screenOptions={{
        // 共通ヘッダ（純粋 UI）に、認証の SignOutButton を app 層で注入して合成する。
        header: ({ options }) => (
          <AppHeader title={options.title} right={<SignOutButton />} />
        ),
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "個人カレンダー",
          tabBarLabel: "個人",
        }}
      />
      <Tabs.Screen
        name="family"
        options={{
          title: "家族カレンダー",
          tabBarLabel: "家族",
        }}
      />
    </Tabs>
  );
}
