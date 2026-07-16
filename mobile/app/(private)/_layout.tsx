import { Tabs } from "expo-router";

export default function PrivateLayout() {
  return (
    <Tabs screenOptions={{ headerShown: true }}>
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
