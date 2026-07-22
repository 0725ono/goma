import { StyleSheet, Text, View } from "react-native";

import { SignInForm } from "@/features/auth/components/SignInForm";

export default function SignInScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>家族スケジュール</Text>
      <Text style={styles.subtitle}>SignIn</Text>
      <SignInForm />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    justifyContent: "center",
    backgroundColor: "#F5F5F5",
  },
  title: {
    fontSize: 28,
    fontWeight: "bold",
    textAlign: "center",
    marginBottom: 10,
    color: "#333",
  },
  subtitle: {
    fontSize: 16,
    textAlign: "center",
    marginBottom: 30,
    color: "#666",
  },
});
