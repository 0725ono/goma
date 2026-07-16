import { StyleSheet, Text, View } from "react-native";

export default function FamilyScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.pageTitle}>家族スケジュール</Text>
      <View style={styles.mockCalendar}>
        <Text style={styles.mockText}>👥 [家族共有カレンダー領域]</Text>
      </View>
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
