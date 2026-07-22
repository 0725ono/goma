import { StyleSheet, Text, View } from "react-native";

const PersonalCalendarView = () => {
  return (
    <View style={styles.featureContainer}>
      <Text style={styles.featureTitle}>
        ここに個人用のカレンダーが表示されます
      </Text>
      <View style={styles.mockCalendar}>
        <Text style={styles.mockText}>📅 [カレンダーライブラリ領域]</Text>
      </View>
    </View>
  );
};

export default function PersonalScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.pageTitle}>個人スケジュール</Text>
      <PersonalCalendarView />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: "#F5F5F5" },
  pageTitle: {
    fontSize: 24,
    fontWeight: "bold",
    marginBottom: 20,
    color: "#333",
  },
  featureContainer: { flex: 1 },
  featureTitle: { fontSize: 16, color: "#666", marginBottom: 10 },
  mockCalendar: {
    flex: 1,
    backgroundColor: "#FFF",
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#DDD",
    alignItems: "center",
    justifyContent: "center",
  },
  mockText: { color: "#999", fontSize: 18, fontWeight: "bold" },
});
